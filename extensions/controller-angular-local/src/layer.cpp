#include "angular_local.h"
#include <openxr/openxr_loader_negotiation.h>
#include <cstring>
#include <memory>
#include <mutex>
#include <unordered_map>

namespace {
constexpr char NAME[] = "XR_APILAYER_local_GalaxyXR_controller_angular_local";
struct Dispatch {
    PFN_xrGetInstanceProcAddr gipa{};
    PFN_xrCreateActionSet createSet{}; PFN_xrDestroyActionSet destroySet{};
    PFN_xrCreateAction createAction{}; PFN_xrDestroyAction destroyAction{};
    PFN_xrCreateSession createSession{}; PFN_xrDestroySession destroySession{};
    PFN_xrCreateActionSpace createSpace{}; PFN_xrDestroySpace destroySpace{};
    PFN_xrLocateSpace locate{}; PFN_xrDestroyInstance destroyInstance{};
};
using D = std::shared_ptr<Dispatch>;
struct Action { D d; XrActionSet set; bool stream; };
struct Space { D d; XrSession session; XrAction action; bool stream; };
std::mutex mutex;
std::unordered_map<XrInstance,D> instances;
std::unordered_map<XrActionSet,D> sets;
std::unordered_map<XrAction,Action> actions;
std::unordered_map<XrSession,D> sessions;
std::unordered_map<XrSpace,Space> spaces;

template<class M, class K> D dispatch(M& map, K key) {
    std::lock_guard<std::mutex> guard(mutex);
    auto it = map.find(key); return it == map.end() ? nullptr : it->second;
}
template<class M, class P> void eraseIf(M& map, P pred) {
    for (auto i=map.begin(); i!=map.end();) { if(pred(i->second)) i=map.erase(i); else ++i; }
}
XrResult XRAPI_PTR createSet(XrInstance instance, const XrActionSetCreateInfo* info, XrActionSet* set) {
    auto d=dispatch(instances,instance); if(!d) return XR_ERROR_HANDLE_INVALID;
    auto r=d->createSet(instance,info,set);
    if(XR_SUCCEEDED(r)) { std::lock_guard<std::mutex> g(mutex); sets[*set]=d; }
    return r;
}
XrResult XRAPI_PTR destroySet(XrActionSet set) {
    auto d=dispatch(sets,set); if(!d) return XR_ERROR_HANDLE_INVALID;
    auto r=d->destroySet(set);
    if(XR_SUCCEEDED(r)) { std::lock_guard<std::mutex> g(mutex); sets.erase(set);
        eraseIf(actions,[&](const Action& a){return a.set==set;}); }
    return r;
}
XrResult XRAPI_PTR createAction(XrActionSet set, const XrActionCreateInfo* info, XrAction* action) {
    auto d=dispatch(sets,set); if(!d) return XR_ERROR_HANDLE_INVALID;
    auto r=d->createAction(set,info,action);
    if(XR_SUCCEEDED(r)) { std::lock_guard<std::mutex> g(mutex);
        actions[*action]={d,set,info->actionType==XR_ACTION_TYPE_POSE_INPUT &&
            std::strcmp(info->actionName,"pamir-stream-pose")==0}; }
    return r;
}
XrResult XRAPI_PTR destroyAction(XrAction action) {
    D d; { std::lock_guard<std::mutex> g(mutex); auto i=actions.find(action);
        if(i!=actions.end()) d=i->second.d; }
    if(!d) return XR_ERROR_HANDLE_INVALID;
    auto r=d->destroyAction(action);
    if(XR_SUCCEEDED(r)) { std::lock_guard<std::mutex> g(mutex); actions.erase(action); }
    return r;
}
XrResult XRAPI_PTR createSession(XrInstance instance, const XrSessionCreateInfo* info, XrSession* session) {
    auto d=dispatch(instances,instance); if(!d) return XR_ERROR_HANDLE_INVALID;
    auto r=d->createSession(instance,info,session);
    if(XR_SUCCEEDED(r)) { std::lock_guard<std::mutex> g(mutex); sessions[*session]=d; }
    return r;
}
XrResult XRAPI_PTR destroySession(XrSession session) {
    auto d=dispatch(sessions,session); if(!d) return XR_ERROR_HANDLE_INVALID;
    auto r=d->destroySession(session);
    if(XR_SUCCEEDED(r)) { std::lock_guard<std::mutex> g(mutex); sessions.erase(session);
        eraseIf(spaces,[&](const Space& s){return s.session==session;}); }
    return r;
}
XrResult XRAPI_PTR createSpace(XrSession session, const XrActionSpaceCreateInfo* info, XrSpace* space) {
    auto d=dispatch(sessions,session); if(!d) return XR_ERROR_HANDLE_INVALID;
    auto r=d->createSpace(session,info,space);
    if(XR_SUCCEEDED(r)) { std::lock_guard<std::mutex> g(mutex); auto a=actions.find(info->action);
        spaces[*space]={d,session,info->action,a!=actions.end() && a->second.d==d && a->second.stream}; }
    return r;
}
// Reference spaces also need dispatch, even though their values are never converted.
using Ref = PFN_xrCreateReferenceSpace;
XrResult XRAPI_PTR createReference(XrSession session, const XrReferenceSpaceCreateInfo* info, XrSpace* space) {
    auto d=dispatch(sessions,session); if(!d) return XR_ERROR_HANDLE_INVALID;
    PFN_xrVoidFunction fn=nullptr;
    XrInstance instance{}; { std::lock_guard<std::mutex> g(mutex);
        for(const auto& item:instances) if(item.second==d) {instance=item.first;break;} }
    auto r=d->gipa(instance,"xrCreateReferenceSpace",&fn); if(XR_FAILED(r)||!fn) return XR_ERROR_FUNCTION_UNSUPPORTED;
    r=reinterpret_cast<Ref>(fn)(session,info,space);
    if(XR_SUCCEEDED(r)) {std::lock_guard<std::mutex> g(mutex); spaces[*space]={d,session,XR_NULL_HANDLE,false};}
    return r;
}
XrResult XRAPI_PTR destroySpace(XrSpace space) {
    D d; {std::lock_guard<std::mutex> g(mutex);auto i=spaces.find(space);if(i!=spaces.end())d=i->second.d;
        // Unknown extension spaces are passed through in Steam Link's single-instance case.
        if(!d&&instances.size()==1)d=instances.begin()->second;}
    if(!d) return XR_ERROR_HANDLE_INVALID;
    auto r=d->destroySpace(space);
    if(XR_SUCCEEDED(r)) {std::lock_guard<std::mutex> g(mutex);spaces.erase(space);}
    return r;
}
XrResult XRAPI_PTR locate(XrSpace space,XrSpace base,XrTime time,XrSpaceLocation* location) {
    D d; bool convert=false;
    {std::lock_guard<std::mutex> g(mutex);auto i=spaces.find(space);
        if(i!=spaces.end()){d=i->second.d;convert=i->second.stream;}
        // Extension-created spaces can be located against a known base. Pass through.
        if(!d){auto b=spaces.find(base);if(b!=spaces.end())d=b->second.d;}
        if(!d&&instances.size()==1)d=instances.begin()->second;}
    if(!d) return XR_ERROR_HANDLE_INVALID;
    auto r=d->locate(space,base,time,location);
    if(XR_SUCCEEDED(r)&&convert) gxrAngularLocal(location);
    return r;
}
XrResult XRAPI_PTR destroyInstance(XrInstance instance) {
    auto d=dispatch(instances,instance);if(!d)return XR_ERROR_HANDLE_INVALID;
    auto r=d->destroyInstance(instance);
    if(XR_SUCCEEDED(r)){std::lock_guard<std::mutex> g(mutex);instances.erase(instance);
        eraseIf(sets,[&](const D& x){return x==d;});eraseIf(sessions,[&](const D& x){return x==d;});
        eraseIf(actions,[&](const Action& x){return x.d==d;});eraseIf(spaces,[&](const Space& x){return x.d==d;});}
    return r;
}
XrResult XRAPI_PTR gipa(XrInstance instance,const char* name,PFN_xrVoidFunction* fn) {
    if(!name||!fn)return XR_ERROR_VALIDATION_FAILURE;
    if(!std::strcmp(name,"xrGetInstanceProcAddr")){*fn=reinterpret_cast<PFN_xrVoidFunction>(gipa);return XR_SUCCESS;}
    auto d=dispatch(instances,instance);if(!d){*fn=nullptr;return XR_ERROR_HANDLE_INVALID;}
    auto r=d->gipa(instance,name,fn);if(XR_FAILED(r)||!*fn)return r;
#define WRAP(api,wrapper) if(!std::strcmp(name,#api))*fn=reinterpret_cast<PFN_xrVoidFunction>(wrapper)
    WRAP(xrCreateActionSet,createSet); WRAP(xrDestroyActionSet,destroySet);
    WRAP(xrCreateAction,createAction); WRAP(xrDestroyAction,destroyAction);
    WRAP(xrCreateSession,createSession); WRAP(xrDestroySession,destroySession);
    WRAP(xrCreateActionSpace,createSpace); WRAP(xrCreateReferenceSpace,createReference);
    WRAP(xrDestroySpace,destroySpace); WRAP(xrLocateSpace,locate); WRAP(xrDestroyInstance,destroyInstance);
#undef WRAP
    return r;
}
XrResult XRAPI_PTR createInstance(const XrInstanceCreateInfo* info,const XrApiLayerCreateInfo* layer,XrInstance* instance) {
    if(!layer||!layer->nextInfo||!layer->nextInfo->nextGetInstanceProcAddr||
       !layer->nextInfo->nextCreateApiLayerInstance)return XR_ERROR_INITIALIZATION_FAILED;
    auto next=*layer;next.nextInfo=layer->nextInfo->next;
    auto r=layer->nextInfo->nextCreateApiLayerInstance(info,&next,instance);if(XR_FAILED(r))return r;
    auto d=std::make_shared<Dispatch>();d->gipa=layer->nextInfo->nextGetInstanceProcAddr;
#define LOAD(api,member) {PFN_xrVoidFunction fn=nullptr;d->gipa(*instance,#api,&fn);d->member=reinterpret_cast<PFN_##api>(fn);}
    LOAD(xrCreateActionSet,createSet);LOAD(xrDestroyActionSet,destroySet);
    LOAD(xrCreateAction,createAction);LOAD(xrDestroyAction,destroyAction);
    LOAD(xrCreateSession,createSession);LOAD(xrDestroySession,destroySession);
    LOAD(xrCreateActionSpace,createSpace);LOAD(xrDestroySpace,destroySpace);
    LOAD(xrLocateSpace,locate);LOAD(xrDestroyInstance,destroyInstance);
#undef LOAD
    if(!d->createSet||!d->destroySet||!d->createAction||!d->destroyAction||!d->createSession||
       !d->destroySession||!d->createSpace||!d->destroySpace||!d->locate||!d->destroyInstance){
        if(d->destroyInstance)d->destroyInstance(*instance);return XR_ERROR_INITIALIZATION_FAILED;}
    {std::lock_guard<std::mutex> g(mutex);instances[*instance]=d;}
    return r;
}
}
#ifdef _WIN32
#define GXR_EXPORT __declspec(dllexport)
#else
#define GXR_EXPORT __attribute__((visibility("default")))
#endif
extern "C" GXR_EXPORT XrResult XRAPI_CALL xrNegotiateLoaderApiLayerInterface(
    const XrNegotiateLoaderInfo* loader,const char* name,XrNegotiateApiLayerRequest* request) {
    if(!loader||!name||!request||std::strcmp(name,NAME)||
       loader->structType!=XR_LOADER_INTERFACE_STRUCT_LOADER_INFO||
       loader->structVersion!=XR_LOADER_INFO_STRUCT_VERSION||loader->structSize!=sizeof(*loader)||
       request->structType!=XR_LOADER_INTERFACE_STRUCT_API_LAYER_REQUEST||
       request->structVersion!=XR_API_LAYER_INFO_STRUCT_VERSION||request->structSize!=sizeof(*request)||
       loader->minInterfaceVersion>XR_CURRENT_LOADER_API_LAYER_VERSION||
       loader->maxInterfaceVersion<XR_CURRENT_LOADER_API_LAYER_VERSION||
       loader->minApiVersion>XR_MAKE_VERSION(1,0,0)||loader->maxApiVersion<XR_MAKE_VERSION(1,0,0))
        return XR_ERROR_INITIALIZATION_FAILED;
    request->layerInterfaceVersion=XR_CURRENT_LOADER_API_LAYER_VERSION;
    request->layerApiVersion=XR_MAKE_VERSION(1,0,0);
    request->getInstanceProcAddr=gipa;request->createApiLayerInstance=createInstance;
    return XR_SUCCESS;
}
