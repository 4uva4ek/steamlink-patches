#pragma once
#include <openxr/openxr.h>
#include <cmath>

// Input is already OpenXR base-space angular velocity. No raw HAL pitch correction.
inline bool gxrAngularLocal(XrSpaceLocation* location) {
    if (!location || !(location->locationFlags & XR_SPACE_LOCATION_ORIENTATION_VALID_BIT)) return false;
    XrSpaceVelocity* velocity = nullptr;
    auto* next = static_cast<XrBaseOutStructure*>(location->next);
    // Bound malformed/cyclic chains and reject duplicate velocity structures.
    unsigned count = 0;
    for (; next && count < 32; next = next->next, ++count) {
        if (next->type == XR_TYPE_SPACE_VELOCITY) {
            if (velocity) return false;
            velocity = reinterpret_cast<XrSpaceVelocity*>(next);
        }
    }
    if (next || !velocity || !(velocity->velocityFlags & XR_SPACE_VELOCITY_ANGULAR_VALID_BIT)) return false;
    const auto q = location->pose.orientation;
    const auto v = velocity->angularVelocity;
    const double norm = double(q.x)*q.x + double(q.y)*q.y + double(q.z)*q.z + double(q.w)*q.w;
    if (!std::isfinite(norm) || std::abs(norm - 1.0) > 0.001 ||
        !std::isfinite(v.x) || !std::isfinite(v.y) || !std::isfinite(v.z)) return false;
    // R(q)^-1 * omega_base, normalized to tolerate normal float quaternion roundoff.
    const double k = 1.0/std::sqrt(norm);
    const double x = -q.x*k, y = -q.y*k, z = -q.z*k, w = q.w*k;
    const double cx = y*v.z-z*v.y, cy = z*v.x-x*v.z, cz = x*v.y-y*v.x;
    const XrVector3f local{
        float(v.x + 2*(w*cx+y*cz-z*cy)),
        float(v.y + 2*(w*cy+z*cx-x*cz)),
        float(v.z + 2*(w*cz+x*cy-y*cx))};
    if (!std::isfinite(local.x) || !std::isfinite(local.y) || !std::isfinite(local.z)) return false;
    velocity->angularVelocity = local;
    return true;
}
