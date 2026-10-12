#include "../src/layer.cpp"
#include <cassert>
#include <iostream>
#include <random>
#include <limits>

static XrResult nextResult=XR_SUCCESS;
static XrSpaceVelocity velocity;
static XrSpaceLocation location;
static unsigned calls=0;
static XrResult XRAPI_PTR mockLocate(XrSpace,XrSpace,XrTime,XrSpaceLocation* out) {
    ++calls; *out=location; return nextResult;
}
static XrResult XRAPI_PTR mockDestroySpace(XrSpace){return nextResult;}
static XrResult XRAPI_PTR mockDestroySession(XrSession){return nextResult;}
static XrResult XRAPI_PTR mockDestroyInstance(XrInstance){return nextResult;}
template<class T> T handle(size_t x){return reinterpret_cast<T>(x);}
static void resetLocation() {
    velocity={XR_TYPE_SPACE_VELOCITY,nullptr,3,{1,2,3},{1,0,0}};
    location={XR_TYPE_SPACE_LOCATION,&velocity,3,{{0,0,float(std::sqrt(.5)),float(std::sqrt(.5))},{4,5,6}}};
}
int main(){
    std::mt19937 rng(20261011);std::normal_distribution<double> normal;
    for(int i=0;i<6000;++i){
        double q[4]; double norm=0;for(double& x:q){x=normal(rng);norm+=x*x;}
        for(double& x:q)x/=std::sqrt(norm);
        resetLocation();location.pose.orientation={float(q[0]),float(q[1]),float(q[2]),float(q[3])};
        velocity.angularVelocity={float(normal(rng)*5),float(normal(rng)*5),float(normal(rng)*5)};
        auto original=velocity;auto pose=location.pose;auto v=velocity.angularVelocity;
        // Independent quaternion rotation matrix, transposed for world -> local.
        double x=q[0],y=q[1],z=q[2],w=q[3];
        double expected[3]={
            (1-2*y*y-2*z*z)*v.x+2*(x*y+z*w)*v.y+2*(x*z-y*w)*v.z,
            2*(x*y-z*w)*v.x+(1-2*x*x-2*z*z)*v.y+2*(y*z+x*w)*v.z,
            2*(x*z+y*w)*v.x+2*(y*z-x*w)*v.y+(1-2*x*x-2*y*y)*v.z};
        assert(gxrAngularLocal(&location));auto a=velocity.angularVelocity;
        assert(std::abs(a.x-expected[0])<1e-5&&std::abs(a.y-expected[1])<1e-5&&std::abs(a.z-expected[2])<1e-5);
        original.angularVelocity=a;assert(!std::memcmp(&original,&velocity,sizeof velocity));
        assert(!std::memcmp(&pose,&location.pose,sizeof pose));
    }
    for(int i=0;i<7;++i){
        resetLocation();XrBaseOutStructure other{XR_TYPE_UNKNOWN,nullptr};
        if(i==0)location.locationFlags=0;
        if(i==1)velocity.velocityFlags=XR_SPACE_VELOCITY_LINEAR_VALID_BIT;
        if(i==2)velocity.angularVelocity.x=std::numeric_limits<float>::quiet_NaN();
        if(i==3)location.pose.orientation.w=2;
        if(i==4)velocity.next=&velocity;
        if(i==5)location.next=nullptr;
        if(i==6){other.next=&other;location.next=&other;}
        auto before=velocity;assert(!gxrAngularLocal(&location));assert(!std::memcmp(&before,&velocity,sizeof velocity));
    }
    auto d=std::make_shared<Dispatch>();d->locate=mockLocate;d->destroySpace=mockDestroySpace;
    d->destroySession=mockDestroySession;d->destroyInstance=mockDestroyInstance;
    auto d2=std::make_shared<Dispatch>(*d);
    auto instance=handle<XrInstance>(1),instance2=handle<XrInstance>(2);
    auto session=handle<XrSession>(3),session2=handle<XrSession>(4);
    auto stream=handle<XrSpace>(5),ref=handle<XrSpace>(6),other=handle<XrSpace>(7);
    instances[instance]=d;instances[instance2]=d2;sessions[session]=d;sessions[session2]=d2;
    spaces[stream]={d,session,XR_NULL_HANDLE,true};spaces[ref]={d,session,XR_NULL_HANDLE,false};
    spaces[other]={d2,session2,XR_NULL_HANDLE,true};
    XrSpaceLocation out{};
    resetLocation();assert(locate(stream,ref,1,&out)==XR_SUCCESS);assert(std::abs(velocity.angularVelocity.y+1)<1e-6);
    resetLocation();assert(locate(ref,stream,1,&out)==XR_SUCCESS);assert(velocity.angularVelocity.x==1);
    resetLocation();nextResult=XR_ERROR_RUNTIME_FAILURE;assert(locate(stream,ref,1,&out)==nextResult);
    assert(velocity.angularVelocity.x==1);assert(destroySpace(stream)==nextResult);assert(spaces.count(stream));
    assert(destroySession(session)==nextResult);assert(sessions.count(session));
    nextResult=XR_SUCCESS;assert(destroySession(session)==XR_SUCCESS);assert(!spaces.count(stream)&&!spaces.count(ref));
    assert(spaces.count(other)&&instances.count(instance2));
    assert(destroyInstance(instance2)==XR_SUCCESS);assert(!spaces.count(other)&&!sessions.count(session2));
    // Unknown extension-created spaces are forwarded, never converted, with one instance.
    resetLocation();assert(locate(stream,ref,1,&out)==XR_SUCCESS);assert(velocity.angularVelocity.x==1);
    assert(destroyInstance(instance)==XR_SUCCESS);
    assert(locate(stream,ref,1,&out)==XR_ERROR_HANDLE_INVALID);
    XrNegotiateLoaderInfo loader{XR_LOADER_INTERFACE_STRUCT_LOADER_INFO,XR_LOADER_INFO_STRUCT_VERSION,sizeof(XrNegotiateLoaderInfo),1,1,XR_MAKE_VERSION(1,0,0),XR_MAKE_VERSION(1,1,43)};
    XrNegotiateApiLayerRequest request{XR_LOADER_INTERFACE_STRUCT_API_LAYER_REQUEST,XR_API_LAYER_INFO_STRUCT_VERSION,sizeof(XrNegotiateApiLayerRequest)};
    assert(xrNegotiateLoaderApiLayerInterface(&loader,NAME,&request)==XR_SUCCESS);
    assert(xrNegotiateLoaderApiLayerInterface(&loader,"wrong",&request)==XR_ERROR_INITIALIZATION_FAILED);
    loader.minInterfaceVersion=2;assert(xrNegotiateLoaderApiLayerInterface(&loader,NAME,&request)==XR_ERROR_INITIALIZATION_FAILED);
    std::cout<<"PASS 6000 independent rotation cases; invalid flags/NaN/quaternion/cyclic chains; stream-only and failed-call pass-through; lifecycle isolation and negotiation\n";
}
