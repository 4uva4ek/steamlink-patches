#include "../src/decoder_probe.h"
#include <cstdio>
#include <limits>
#include <stdexcept>
#include <vector>

static int checks = 0, failures = 0;
#define CHECK(expression) do { ++checks; if (!(expression)) { ++failures; \
    std::printf("FAIL line %d: %s\n", __LINE__, #expression); } } while (0)

int main() {
    // Mock actual NDK boundary: a codec helper has already changed the buffer
    // and selected a replacement handle before it calls this observed API.
    // The production proxy uses this exact once-only observation helper.
    gxr::DecoderProbeState actualState;
    CHECK(actualState.AdmitVideo(40, "video/hevc")); // factory before PC lease
    CHECK(actualState.AdmitVideo(41, "video/av01")); // replacement factory
    actualState.Session(std::string(32, 'd'));
    std::vector<uint8_t> buffer{0, 0, 1, 0x42, 1}, exported, received;
    int calls = 0;
    auto actualQueue = [&](uintptr_t handle, int status) {
        return gxr::ProbeObservedCall(
            [&] { return buffer; },
            [&] { ++calls; received = buffer; return status; },
            [&](int result, std::vector<uint8_t> snapshot) {
                actualState.Queue(handle, 99, snapshot.size(), 0, result, 1000);
                if (!result && actualState.IsVideo(handle)) exported = std::move(snapshot);
            });
    };
    buffer = {0x12, 0, 0x0a, 1, 7}; // helper rewrite before actual API entry
    CHECK(actualQueue(41, 0) == 0 && calls == 1);
    CHECK(exported == buffer && received == buffer);
    CHECK(actualState.counters.accepted_input_bytes == buffer.size());
    actualState.Output(41, 99, 0, 1500);
    CHECK(actualState.counters.queue_to_output_count == 1);
    exported.clear();
    CHECK(actualQueue(41, -10000) == -10000 && calls == 2 && exported.empty());
    CHECK(actualState.counters.queue_failures == 1 && actualState.counters.accepted_input_bytes == buffer.size());
    CHECK(actualQueue(99, 0) == 0 && calls == 3 && exported.empty()); // unadmitted/audio ignored
    int commits = 0;
    CHECK(gxr::ProbeObservedCall(
        []() -> int { throw std::runtime_error("diagnostic allocation failed"); },
        [&] { ++calls; return -27; },
        [&](int status, int snapshot) { ++commits; CHECK(status == -27 && snapshot == 0); }) == -27);
    CHECK(calls == 4 && commits == 1);
    CHECK(gxr::ProbeObservedCall(
        [] { return 1; }, [&] { ++calls; return 0; },
        [](int, int) { throw std::runtime_error("optional export failed"); }) == 0);
    CHECK(calls == 5); // diagnostics never retry or rewrite the real API result

    const std::string token(32, 'a'), other(32, 'b'), nonce(32, 'c');
    std::string parsed, requestNonce;
    CHECK(gxr::ProbeRequest(token + "\n", parsed) && parsed == token);
    CHECK(gxr::ProbeRequest(token + " " + nonce, parsed, &requestNonce) && requestNonce == nonce);
    CHECK(!gxr::ProbeRequest(token + " garbage", parsed));
    CHECK(!gxr::ProbeRequest(token + " " + nonce + " extra", parsed, &requestNonce));
    CHECK(!gxr::ProbeToken(std::string(32, 'g')));
    CHECK(!gxr::ProbeToken(std::string(31, 'a')));
    CHECK(gxr::ProbeBufferRange(100, 20, 80));
    CHECK(!gxr::ProbeBufferRange(100, -1, 2));
    CHECK(!gxr::ProbeBufferRange(100, 101, 0));
    CHECK(!gxr::ProbeBufferRange(100, 99, 2));
    CHECK(!gxr::ProbeBufferRange(std::numeric_limits<size_t>::max(), 0, gxr::kProbeMaxCapture + 1));
    const uint8_t av1[] = {0x12, 0, 0x0a, 1, 0};
    const uint8_t avc[] = {0, 0, 0, 1, 0x67, 0};
    const uint8_t hevc[] = {0, 0, 1, 0x42, 1};
    CHECK(gxr::ProbeHasSequenceHeader(av1, sizeof(av1)));
    CHECK(gxr::ProbeHasSequenceHeader(avc, sizeof(avc)));
    CHECK(gxr::ProbeHasSequenceHeader(hevc, sizeof(hevc)));
    CHECK(!gxr::ProbeHasSequenceHeader(avc, 4));

    gxr::DecoderProbeState state;
    CHECK(state.AdmitVideo(1, "video/hevc") && state.AdmitVideo(2, "video/av01"));
    CHECK(!state.AdmitVideo(9, "audio/opus") && !state.AdmitVideo(10, "video/"));
    CHECK(!state.AdmitVideo(0, "video/avc") && !state.AdmitVideo(10, nullptr));
    CHECK(!state.IsVideo(9) && state.IsVideo(1));
    state.Queue(1, 1, 100, 0, 0, 1000);
    CHECK(state.counters.accepted_input_bytes == 0);
    CHECK(state.Session(token));
    uint64_t generation = state.counters.stream;
    state.Queue(9, 7, 999, 0, 0, 1000);
    state.Output(9, 7, 0, 1500);
    CHECK(state.counters.accepted_input_bytes == 0 && state.counters.output_frames == 0);
    CHECK(!state.ForgetVideo(9) && state.counters.stream == generation);
    CHECK(!state.Session(token) && state.counters.stream == generation);
    state.Queue(1, 7, 100, 0, -10000, 1000);
    state.Queue(1, 7, 100, 2, 0, 1000);
    state.Queue(1, 7, 100, 4, 0, 1000);
    state.Queue(1, 7, 0, 0, 0, 1000);
    CHECK(state.counters.queue_failures == 1 && state.counters.queue_successes == 3);
    CHECK(state.counters.accepted_input_bytes == 0 && state.PendingCount() == 0);
    state.Queue(1, 7, 100, 0, 0, 1000);
    state.Output(2, 7, 0, 1500); // same PTS in another codec must not match
    CHECK(state.counters.queue_to_output_count == 0);
    state.Output(1, 7, 0, 2000);
    CHECK(state.counters.accepted_input_bytes == 100 && state.counters.accepted_input_frames == 1);
    CHECK(state.counters.queue_to_output_count == 1 && state.counters.queue_to_output_sum_ns == 1000);
    CHECK(state.counters.queue_to_output_max_ns == 1000 && state.PendingCount() == 0);
    state.Output(1, 7, 0, 2500); // no second match
    CHECK(state.counters.queue_to_output_count == 1);
    state.Queue(1, 8, 50, 0, 0, 3000);
    state.Queue(1, 8, 50, 0, 0, 3100);
    state.Output(1, 8, 0, 4000);
    state.Queue(1, 8, 50, 0, 0, 4100); // keep ambiguity through outstanding duplicates
    state.Output(1, 8, 0, 4200);
    state.Output(1, 8, 0, 4300);
    CHECK(state.counters.queue_to_output_count == 1 && state.PendingCount() == 0);
    CHECK(state.counters.ambiguous_pts == 2);
    state.Queue(1, 9, 50, 0, 0, 5000);
    state.Output(1, 9, 0, 4999); // a reversed clock must not underflow
    CHECK(state.counters.queue_to_output_count == 1);
    state.Output(1, 10, 2, 6000); // config/EOS are not frames
    state.Output(1, 10, 4, 6000);
    CHECK(state.counters.output_frames == 7);
    state.Queue(1, 11, 25, 8, 0, 6000); // partial input counts bytes, not complete frames
    CHECK(state.counters.accepted_input_bytes == 325 && state.counters.accepted_input_frames == 5);
    state.Reset(); // flush/stop/delete generation
    CHECK(state.counters.stream == generation + 1 && state.PendingCount() == 0);
    CHECK(state.counters.accepted_input_bytes == 0 && state.counters.queue_to_output_count == 0);
    for (size_t i = 0; i < gxr::kProbeMaxPending + 10; ++i)
        state.Queue(1, i, 10, 0, 0, i + 1);
    CHECK(state.PendingCount() == gxr::kProbeMaxPending && state.counters.pending_evictions == 10);
    CHECK(state.Session(other) && state.PendingCount() == 0 && state.counters.accepted_input_bytes == 0);
    CHECK(state.Session("") && state.PendingCount() == 0);
    state.Output(1, 0, 0, 6000);
    CHECK(state.counters.output_frames == 0);
    CHECK(state.ForgetVideo(1) && !state.IsVideo(1) && !state.ForgetVideo(1));
    CHECK(state.AdmitVideo(3, "video/avc")); // live swap target explicitly re-admitted
    state.Session(token);
    state.Queue(1, 0, 999, 0, 0, 6000); // retired handle cannot contaminate the live stream
    state.Queue(3, 0, 50, 0, 0, 6000);
    state.Output(1, 0, 0, 6500);
    state.Output(3, 0, 0, 6500);
    CHECK(state.counters.accepted_input_bytes == 50 && state.counters.output_frames == 1);
    CHECK(state.counters.queue_to_output_count == 1);
    gxr::DecoderProbeState boundedScope;
    for (uintptr_t handle = 1; handle <= 16; ++handle) CHECK(boundedScope.AdmitVideo(handle, "video/avc"));
    CHECK(!boundedScope.AdmitVideo(17, "video/avc"));
    std::printf("Decoder probe: %d checks, %d failures\n", checks, failures);
    return failures ? 1 : 0;
}
