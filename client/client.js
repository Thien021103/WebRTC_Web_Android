const iceConnectionLog = document.getElementById('ice-connection-state'),
    iceGatheringLog = document.getElementById('ice-gathering-state'),
    signalingLog = document.getElementById('signaling-state'),
    dataChannelLog = document.getElementById('data-channel'),
    dtlsConnectionLog = document.getElementById('dtls-connection-state');

const websocket = new WebSocket('ws://127.0.0.1:8000/');
let pc = null;
let datachannel = null;
let receivedOffer = false;
let currentState = null;
let manualStream = new MediaStream();

websocket.onopen = () => {
    console.log('WebSocket connection established');
};

websocket.onmessage = async (evt) => {
    if (typeof evt.data !== 'string') {
        return;
    }

    const message = evt.data.trim();

    if (message.startsWith("OFFER")) {
        const sdp = message.substring(6); // Extract SDP after "OFFER "
        receivedOffer = true;
        console.log(`Sending Offer and setting local\nreceivedOffer state: ${receivedOffer}`);
        await handleOffer(sdp);
        await sendMedia(pc)
        if (currentState === "Creating") {
            await sendAnswer(pc);
        }
    } else if (message.startsWith("STATE")) {
        const [_, state, ...details] = message.split(" ");
        handleStateMessage(state, details.join(" "));
    } else if (message.startsWith("ICE")) {
        const candidate = message.substring(4); // Extract ICE candidate after "ICE "
        await handleIceMessage(candidate);
    } else {
        console.error("Unrecognized or unexpected message:", message);
    }
};

// Modify createPeerConnection to send ICE candidates as they are generated
function createPeerConnection() {
    const config = {
        bundlePolicy: "max-bundle",
    };

    if (document.getElementById('use-stun').checked) {
        config.iceServers = [
            { urls: ['stun:stun.l.google.com:19302'] },
        ];
    }

    let pc = new RTCPeerConnection(config);

    pc.addEventListener('iceconnectionstatechange', () =>
        iceConnectionLog.textContent += ' -> ' + pc.iceConnectionState);
    iceConnectionLog.textContent = pc.iceConnectionState;

    pc.addEventListener('icegatheringstatechange', () =>
        iceGatheringLog.textContent += ' -> ' + pc.iceGatheringState);
    iceGatheringLog.textContent = pc.iceGatheringState;

    pc.addEventListener('signalingstatechange', () =>
        signalingLog.textContent += ' -> ' + pc.signalingState);
    signalingLog.textContent = pc.signalingState;

    // Add event listeners for DTLS connection state changes
    pc.addEventListener('dtlsconnectionstatechange', () => {
        dtlsConnectionLog.textContent += ' -> ' + pc.dtlsConnectionState;
    });
    dtlsConnectionLog.textContent = pc.dtlsConnectionState;

    setInterval(() => { 
        pc.getStats(null).then((stats) => {
            let statsOutput = "";
            stats.forEach((report) => {
            statsOutput +=
                `<h2>Report: ${report.type}</h2>\n<strong>ID:</strong> ${report.id}<br>\n` +
                `<strong>Timestamp:</strong> ${report.timestamp}<br>\n`;
            // Now the statistics for this report; we intentionally drop the ones we
            // sorted to the top above

            Object.keys(report).forEach((statName) => {
                if (
                    statName !== "id" &&
                    statName !== "timestamp" &&
                    statName !== "type"
                ) {
                    statsOutput += `<strong>${statName}:</strong> ${report[statName]}<br>\n`;
                }
            });
            });

            document.getElementById("stats-box").innerHTML = statsOutput;
        });
    }, 1000);

    pc.onicecandidate = (event) => {
        if (event.candidate) {
            const iceCandidate = event.candidate;
            const iceMessage = `ICE ${iceCandidate.sdpMid}$${iceCandidate.sdpMLineIndex}$${iceCandidate.candidate}`;
            websocket.send(iceMessage);
        }
    };

    pc.ontrack = (evt) => {
        console.log('Received track: ', evt.track);
        console.log('Streams attached:', evt.streams);

        manualStream.addTrack(evt.track); // Add the received track to the stream

        document.getElementById('media').style.display = 'block';
        const video = document.getElementById('remote-video');
        video.srcObject = manualStream;
        if(receivedOffer){
            console.log('Playing stream', manualStream);
            video.play();
        }
    };

    datachannel = pc.createDataChannel('pear');

    datachannel.onclose = () => console.log('Data channel has closed');
    datachannel.onopen = () => {
        console.log('Data channel has opened');
        setInterval(() => {
            console.log('Sending ping');
            datachannel.send('ping');
        }, 1000);
    };

    datachannel.onmessage = async (e) => {
        if (e.data instanceof Blob) {
            const buffer = await e.data.arrayBuffer();
            const blob = new Blob([new Uint8Array(buffer)], { type: "image/jpeg" });
            const imageUrl = URL.createObjectURL(blob);
            document.getElementById('imgStream').src = imageUrl;
        }
    };

    return pc;
}

// Sending video/audio
async function sendMedia(pc) {
    try {
        // Capture audio and video from the user's device
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true });
        // Add our local video for better viewing
        const localVideo = document.getElementById('local-video');
        localVideo.srcObject = stream;

        // Add each track (audio and video) to the PeerConnection
        stream.getTracks().forEach((track) => pc.addTrack(track, stream));
        console.log('Media tracks added:', stream.getTracks());
    } catch (error) {
        console.error('Error accessing media devices:', error);
    }
}


// Handle incoming STATE messages
async function handleStateMessage(state, details) {
    console.log(`State: ${state}, Details: ${details}\nreceivedOffer state: ${receivedOffer}`);
    currentState = state;
}

// Handle incoming ICE candidates
async function handleIceMessage(message) {
    const parts = message.split('$');
    if (parts.length === 3) {
        const candidate = {
            sdpMid: parts[0],
            sdpMLineIndex: parseInt(parts[1], 10),
            candidate: parts[2],
        };
        try {
            await pc.addIceCandidate(candidate);
            console.log('ICE candidate added:', candidate);
        } catch (err) {
            console.error('Error adding ICE candidate:', err);
        }
    } else {
        console.error('Invalid ICE message format:', message);
    }
}


async function waitGatheringComplete() {
    return new Promise((resolve) => {
        if (pc.iceGatheringState === 'complete') {
            resolve();
        } else {
            pc.addEventListener('icegatheringstatechange', () => {
                if (pc.iceGatheringState === 'complete') {
                    resolve();
                }
            });
        }
    });
}

async function sendAnswer(pc) {
    await pc.setLocalDescription(await pc.createAnswer());
    await waitGatheringComplete();
    const answer = pc.localDescription;
    document.getElementById('answer-sdp').textContent = answer.sdp;

    websocket.send(`ANSWER ${answer.sdp}`);
}

// Handle incoming OFFER
async function handleOffer(sdp) {
    // Add newline to the end of the SDP string
    sdp += '\n';
    document.getElementById('offer-sdp').textContent = sdp;
    // Proceed with setting the remote description
    pc = createPeerConnection();
    await pc.setRemoteDescription({ type: "offer", sdp });
}

