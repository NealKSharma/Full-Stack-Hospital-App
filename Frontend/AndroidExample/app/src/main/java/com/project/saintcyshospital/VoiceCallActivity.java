package com.project.saintcyshospital;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;



import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.project.saintcyshospital.ws.ChatWsHub;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.CameraEnumerator;

import java.util.ArrayList;
import java.util.List;

public class VoiceCallActivity extends BaseActivity {

    private static final int REQ_PERMISSIONS = 1001;

    private String conversationId;
    private boolean isCaller;

    private EglBase eglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private VideoCapturer videoCapturer;
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;

    private SurfaceViewRenderer localView;
    private SurfaceViewRenderer remoteView;

    private View callStatusContainer;
    private TextView callStatusText;
    private Animation callStatusAnimation;

    private boolean hasLocalSdp = false;
    private SurfaceTextureHelper surfaceTextureHelper;
    private static boolean webrtcInitialized = false;

    private static final long RING_TIMEOUT_MS = 15_000L;

    private final Handler ringHandler = new Handler(Looper.getMainLooper());
    private Runnable ringTimeoutRunnable;

    private boolean callConnected = false;
    private boolean callEnded = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);

        conversationId = getIntent().getStringExtra("conversationId");
        if (conversationId == null) conversationId = "unknown";
        isCaller = getIntent().getBooleanExtra("isCaller", true);

        localView = findViewById(R.id.local_view);
        remoteView = findViewById(R.id.remote_view);
        FrameLayout endBtn = findViewById(R.id.btn_end_call_container);

        callStatusContainer = findViewById(R.id.call_status_container);
        callStatusText = findViewById(R.id.call_status_text);
        setupCallStatusUi();

        ringTimeoutRunnable = () -> {
            if (isFinishing() || callConnected || callEnded) return;

            NotificationManager nm =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (MyFirebaseMessagingService.lastCallNotificationId != null) {
                nm.cancel(MyFirebaseMessagingService.lastCallNotificationId);
                MyFirebaseMessagingService.lastCallNotificationId = null;
            }

            showCallStatus("User not available");
            Toast.makeText(this, "The other person is not available.", Toast.LENGTH_SHORT).show();

            endCall();
        };

        endBtn.setOnClickListener(v -> endCall());

        Toast.makeText(this, "VoiceCallActivity onCreate", Toast.LENGTH_SHORT).show();

        if (hasPermissions()) {
            startWebRtc();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    REQ_PERMISSIONS
            );
        }

        ChatWsHub.get().setCallSignalListener((type, json) -> {
            try {
                JSONObject j = new JSONObject(json);
                String roomId = j.optString("roomId", "");
                if (!conversationId.equals(roomId)) {
                    return;
                }

                String from = j.optString("from", "");
                String me = getMyUsername();
                if (me != null && !me.isEmpty() && me.equals(from)) {
                    return;
                }

                switch (type) {
                    case "CALL_OFFER":
                        handleRemoteOffer(j);
                        break;
                    case "CALL_ANSWER":
                        handleRemoteAnswer(j);
                        break;
                    case "CALL_ICE":
                        handleRemoteIce(j);
                        break;
                    case "CALL_END":
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Remote ended call", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                        break;
                    case "CALL_READY":
                        if (isCaller && peerConnection != null) {
                            runOnUiThread(() -> {
                                createAndSendOffer();
                                ringHandler.removeCallbacks(ringTimeoutRunnable);
                                ringHandler.postDelayed(ringTimeoutRunnable, RING_TIMEOUT_MS);
                            });
                        }
                        break;
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ChatWsHub.get().setCallActive(true);
        ChatWsHub.get().start(getApplicationContext(), conversationId);

        if (!isCaller) {
            sendReady();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        ChatWsHub.get().setCallActive(false);
        ChatWsHub.get().stop();
        disposeWebRtc();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ChatWsHub.get().clearCallSignalListener();
        ringHandler.removeCallbacks(ringTimeoutRunnable);
        disposeWebRtc();
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_PERMISSIONS) {
            if (hasPermissions()) {
                Toast.makeText(this, "Permissions granted, starting WebRTC", Toast.LENGTH_SHORT).show();
                startWebRtc();
            } else {
                Toast.makeText(this,
                        "Camera & mic are required for calls. Enable them in Settings.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startWebRtc() {
        try {
            Log.d("VoiceCallActivity", "startWebRtc: initPeerConnectionFactory");
            initPeerConnectionFactory();
        } catch (Exception e) {
            Log.e("VoiceCallActivity", "initPeerConnectionFactory failed", e);
            Toast.makeText(this, "Failed to init WebRTC factory", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Log.d("VoiceCallActivity", "startWebRtc: initVideoAndAudio");
            initVideoAndAudio();
        } catch (Exception e) {
            Log.e("VoiceCallActivity", "initVideoAndAudio failed", e);
            Toast.makeText(this, "Failed to set up camera/mic", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Log.d("VoiceCallActivity", "startWebRtc: createPeerConnection");
            createPeerConnection();
        } catch (Exception e) {
            Log.e("VoiceCallActivity", "createPeerConnection failed", e);
            Toast.makeText(this, "Failed to create peer connection", Toast.LENGTH_LONG).show();
            return;
        }

        if (peerConnection == null) {
            Toast.makeText(this, "Peer connection is null", Toast.LENGTH_LONG).show();
            return;
        }

        if (isCaller) {
            Log.d("VoiceCallActivity", "startWebRtc: creating offer as caller");
            createAndSendOffer();

            ringHandler.postDelayed(ringTimeoutRunnable, RING_TIMEOUT_MS);
        }

    }


    private void initPeerConnectionFactory() {
        if (!webrtcInitialized) {
            PeerConnectionFactory.InitializationOptions initOptions =
                    PeerConnectionFactory.InitializationOptions.builder(getApplicationContext())
                            .createInitializationOptions();
            PeerConnectionFactory.initialize(initOptions);
            webrtcInitialized = true;
        }

        eglBase = EglBase.create();

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        boolean enableHw = !isRunningOnEmulator();

        DefaultVideoEncoderFactory encoderFactory =
                new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), enableHw, enableHw);

        DefaultVideoDecoderFactory decoderFactory =
                new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }


    private void initVideoAndAudio() {
        if (peerConnectionFactory == null || eglBase == null) {
            throw new IllegalStateException("PeerConnectionFactory or EglBase not initialized");
        }

        if (localView != null) {
            localView.init(eglBase.getEglBaseContext(), null);
            localView.setZOrderMediaOverlay(true);

            localView.setEnableHardwareScaler(true);
            if (isRunningOnEmulator()) {
                localView.setFpsReduction(5f); // ~5 fps
            }
        }
        if (remoteView != null) {
            remoteView.init(eglBase.getEglBaseContext(), null);
            remoteView.setEnableHardwareScaler(true);
            if (isRunningOnEmulator()) {
                remoteView.setFpsReduction(5f);
            }
        }

        try {
            videoCapturer = createVideoCapturer();
            if (videoCapturer != null) {
                surfaceTextureHelper =
                        SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());

                videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
                videoCapturer.initialize(
                        surfaceTextureHelper,
                        getApplicationContext(),
                        videoSource.getCapturerObserver()
                );

                int width  = isRunningOnEmulator() ? 160 : 320;
                int height = isRunningOnEmulator() ? 120 : 240;
                int fps    = isRunningOnEmulator() ? 5   : 15;

                try {
                    videoCapturer.startCapture(width, height, fps);
                } catch (Exception e) {
                    Log.e("VoiceCallActivity", "startCapture failed", e);
                }

                localVideoTrack = peerConnectionFactory.createVideoTrack("LOCAL_VIDEO", videoSource);
                if (localView != null) {
                    localView.setMirror(true);
                    localVideoTrack.addSink(localView);
                }
            } else {
                Log.w("VoiceCallActivity", "No video capturer, running audio-only");
                Toast.makeText(this, "No camera available, audio-only call", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("VoiceCallActivity", "Video init failed, falling back to audio-only", e);
            Toast.makeText(this, "Video failed, falling back to audio-only", Toast.LENGTH_SHORT).show();
            videoCapturer = null;
            videoSource = null;
            localVideoTrack = null;
        }

        MediaConstraints audioConstraints = new MediaConstraints();
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("LOCAL_AUDIO", audioSource);
    }



    private VideoCapturer createVideoCapturer() {
        VideoCapturer capturer = null;

        if (Camera2Enumerator.isSupported(this)) {
            Camera2Enumerator enumerator = new Camera2Enumerator(this);
            capturer = createCameraCapturer(enumerator);
            if (capturer != null) return capturer;
        }

        Camera1Enumerator enumerator1 = new Camera1Enumerator(true);
        capturer = createCameraCapturer(enumerator1);

        return capturer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        for (String deviceName : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) {
                    Log.d("VoiceCallActivity", "Using front camera: " + deviceName);
                    return capturer;
                }
            }
        }

        for (String deviceName : enumerator.getDeviceNames()) {
            VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
            if (capturer != null) {
                Log.d("VoiceCallActivity", "Using fallback camera: " + deviceName);
                return capturer;
            }
        }

        Log.e("VoiceCallActivity", "No camera capturer found at all");
        return null;
    }


    private void createPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = peerConnectionFactory.createPeerConnection(
                rtcConfig,
                new PeerConnection.Observer() {
                    @Override
                    public void onSignalingChange(PeerConnection.SignalingState newState) {
                    }

                    @Override
                    public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
                        android.util.Log.d("WEBRTC_STATE",
                                "IceConnectionState=" + newState
                                        + " room=" + conversationId
                                        + " me=" + getMyUsername());
                        if (newState == PeerConnection.IceConnectionState.CONNECTED) {
                            callConnected = true;
                            ringHandler.removeCallbacks(ringTimeoutRunnable);
                            runOnUiThread(this::hideCallStatusSafe);
                        } else if (newState == PeerConnection.IceConnectionState.FAILED
                                || newState == PeerConnection.IceConnectionState.DISCONNECTED) {
                            runOnUiThread(() -> showCallStatus("Connection lost"));
                        }
                    }



                    @Override
                    public void onIceConnectionReceivingChange(boolean receiving) {
                    }

                    @Override
                    public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
                    }

                    @Override
                    public void onIceCandidate(IceCandidate iceCandidate) {
                        sendIceCandidate(iceCandidate);
                    }

                    @Override
                    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
                    }

                    @Override
                    public void onAddStream(MediaStream mediaStream) {
                    }

                    @Override
                    public void onRemoveStream(MediaStream mediaStream) {
                    }

                    @Override
                    public void onDataChannel(org.webrtc.DataChannel dataChannel) {
                    }

                    @Override
                    public void onRenegotiationNeeded() {
                    }

                    @Override
                    public void onAddTrack(org.webrtc.RtpReceiver receiver, MediaStream[] mediaStreams) {
                    }

                    @Override
                    public void onTrack(org.webrtc.RtpTransceiver transceiver) {
                        org.webrtc.MediaStreamTrack track = transceiver.getReceiver().track();
                        if (track instanceof VideoTrack && remoteView != null) {
                            VideoTrack videoTrack = (VideoTrack) track;
                            videoTrack.addSink(remoteView);

                            runOnUiThread(this::hideCallStatusSafe);
                        }
                    }
                    private void hideCallStatusSafe() {
                        hideCallStatus();
                    }
                }
        );

        if (peerConnection == null) {
            Log.e("VoiceCallActivity", "Failed to create PeerConnection");
            return;
        }

        List<String> streamIds = java.util.Collections.singletonList("LOCAL_STREAM");

        if (localAudioTrack != null) {
            peerConnection.addTrack(localAudioTrack, streamIds);
        }

        if (localVideoTrack != null) {
            peerConnection.addTrack(localVideoTrack, streamIds);
        }
    }

    private boolean isRunningOnEmulator() {
        String fingerprint = Build.FINGERPRINT;
        String model = Build.MODEL;
        String product = Build.PRODUCT;
        String manufacturer = Build.MANUFACTURER;

        return (fingerprint != null && (fingerprint.startsWith("generic") || fingerprint.startsWith("unknown")))
                || (model != null && (model.contains("Emulator") || model.contains("Android SDK built for x86")))
                || (product != null && product.contains("sdk_gphone"))
                || (manufacturer != null && manufacturer.contains("Genymotion"));
    }


    private void createAndSendOffer() {
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                hasLocalSdp = true;
                sendOffer(sessionDescription);
            }

            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String s) {}
            @Override public void onSetFailure(String s) {}
        }, sdpConstraints);
    }

    private void sendOffer(SessionDescription sdp) {
        try {
            android.util.Log.d("WEBRTC_SIGNAL_TX",
                    "sendOffer: room=" + conversationId
                            + " from=" + getMyUsername()
                            + " length=" + sdp.description.length());
            JSONObject j = new JSONObject()
                    .put("type", "CALL_OFFER")
                    .put("roomId", conversationId)
                    .put("from", getMyUsername())
                    .put("sdp", sdp.description)
                    .put("sdpType", sdp.type.canonicalForm());
            ChatWsHub.get().send(j.toString());
        } catch (Exception ignored) {}
    }

    private void sendAnswer(SessionDescription sdp) {
        try {
            android.util.Log.d("WEBRTC_SIGNAL_TX",
                    "sendAnswer: room=" + conversationId
                            + " from=" + getMyUsername()
                            + " length=" + sdp.description.length());
            JSONObject j = new JSONObject()
                    .put("type", "CALL_ANSWER")
                    .put("roomId", conversationId)
                    .put("from", getMyUsername())
                    .put("sdp", sdp.description)
                    .put("sdpType", sdp.type.canonicalForm());
            ChatWsHub.get().send(j.toString());
        } catch (Exception ignored) {}
    }

    private void sendIceCandidate(IceCandidate ice) {
        try {
            android.util.Log.d("WEBRTC_SIGNAL_TX",
                    "sendIce: room=" + conversationId
                            + " from=" + getMyUsername()
                            + " mid=" + ice.sdpMid
                            + " index=" + ice.sdpMLineIndex);
            JSONObject j = new JSONObject()
                    .put("type", "CALL_ICE")
                    .put("roomId", conversationId)
                    .put("from", getMyUsername())
                    .put("sdpMid", ice.sdpMid)
                    .put("sdpMLineIndex", ice.sdpMLineIndex)
                    .put("candidate", ice.sdp);
            ChatWsHub.get().send(j.toString());
        } catch (Exception ignored) {}
    }

    private void sendEnd() {
        try {
            JSONObject j = new JSONObject()
                    .put("type", "CALL_END")
                    .put("roomId", conversationId)
                    .put("from", getMyUsername());
            ChatWsHub.get().send(j.toString());
        } catch (Exception ignored) {}
    }

    private void handleRemoteOffer(JSONObject j) {
        try {
            android.util.Log.d("WEBRTC_SIGNAL_RX",
                    "handleRemoteOffer: room=" + conversationId
                            + " from=" + j.optString("from","?")
                            + " sdpLen=" + j.optString("sdp","").length());
            final String sdpStr = j.optString("sdp", "");
            final String sdpType = j.optString("sdpType", "offer");
            if (peerConnection == null || sdpStr.isEmpty()) return;

            SessionDescription offer =
                    new SessionDescription(SessionDescription.Type.fromCanonicalForm(sdpType), sdpStr);

            peerConnection.setRemoteDescription(new SimpleSdpObserver(), offer);

            MediaConstraints sdpConstraints = new MediaConstraints();
            sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
            sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

            peerConnection.createAnswer(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                    hasLocalSdp = true;
                    sendAnswer(sessionDescription);
                }

                @Override public void onSetSuccess() {}
                @Override public void onCreateFailure(String s) {}
                @Override public void onSetFailure(String s) {}
            }, sdpConstraints);

        } catch (Exception ignored) {}
    }

    private void handleRemoteAnswer(JSONObject j) {
        try {
            android.util.Log.d("WEBRTC_SIGNAL_RX",
                    "handleRemoteAnswer: room=" + conversationId
                            + " from=" + j.optString("from","?")
                            + " sdpLen=" + j.optString("sdp","").length());
            final String sdpStr = j.optString("sdp", "");
            final String sdpType = j.optString("sdpType", "answer");
            if (peerConnection == null || sdpStr.isEmpty()) return;

            SessionDescription answer =
                    new SessionDescription(SessionDescription.Type.fromCanonicalForm(sdpType), sdpStr);

            peerConnection.setRemoteDescription(new SimpleSdpObserver(), answer);
        } catch (Exception ignored) {}
    }

    private void handleRemoteIce(JSONObject j) {
        try {
            android.util.Log.d("WEBRTC_SIGNAL_RX",
                    "handleRemoteIce: room=" + conversationId
                            + " from=" + j.optString("from","?")
                            + " mid=" + j.optString("sdpMid","null")
                            + " idx=" + j.optInt("sdpMLineIndex",-1));
            String sdpMid = j.optString("sdpMid", null);
            int sdpMLineIndex = j.optInt("sdpMLineIndex", -1);
            String candidate = j.optString("candidate", null);

            if (peerConnection == null || sdpMid == null || candidate == null || sdpMLineIndex < 0) {
                return;
            }

            IceCandidate ice = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
            peerConnection.addIceCandidate(ice);
        } catch (Exception ignored) {}
    }

    private void endCall() {
        callEnded = true;
        ringHandler.removeCallbacks(ringTimeoutRunnable);
        sendEnd();
        disposeWebRtc();
        finish();
    }


    private void disposeWebRtc() {
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (Exception ignored) {}
            videoCapturer.dispose();
            videoCapturer = null;
        }

        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }

        if (localView != null) {
            localView.clearImage();
            localView.release();
        }
        if (remoteView != null) {
            remoteView.clearImage();
            remoteView.release();
        }
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection.dispose();
            peerConnection = null;
        }
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }
    }

    private String getMyUsername() {
        SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        return prefs.getString("last_user", "");
    }

    private void setupCallStatusUi() {
        if (callStatusText == null || callStatusContainer == null) return;

        AlphaAnimation pulse = new AlphaAnimation(0.4f, 1.0f);
        pulse.setDuration(800);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        callStatusAnimation = pulse;

        // Initial text based on role
        if (isCaller) {
            showCallStatus("Calling...");
        } else {
            showCallStatus("Connecting...");
        }
    }

    private void showCallStatus(String text) {
        if (callStatusContainer == null || callStatusText == null) return;
        callStatusText.setText(text);
        callStatusContainer.setVisibility(View.VISIBLE);
        if (callStatusAnimation != null) {
            callStatusText.startAnimation(callStatusAnimation);
        }
    }

    private void hideCallStatus() {
        if (callStatusContainer == null || callStatusText == null) return;
        callStatusText.clearAnimation();
        callStatusContainer.setVisibility(View.GONE);
    }

    private void sendReady() {
        try {
            JSONObject j = new JSONObject()
                    .put("type", "CALL_READY")
                    .put("roomId", conversationId)
                    .put("from", getMyUsername());
            ChatWsHub.get().send(j.toString());
        } catch (Exception ignored) {}
    }

    private static class SimpleSdpObserver implements SdpObserver {
        @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
        @Override public void onSetSuccess() {}
        @Override public void onCreateFailure(String s) {}
        @Override public void onSetFailure(String s) {}
    }
}
