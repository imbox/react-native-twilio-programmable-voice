import {
    NativeModules,
    NativeEventEmitter,
    Platform,
} from 'react-native'

const ANDROID = 'android'
const IOS = 'ios'

const TwilioVoice = NativeModules.RNTwilioVoice

const NativeAppEventEmitter = new NativeEventEmitter(TwilioVoice)

const _eventHandlers = {
    deviceReady: new Map(),
    deviceNotReady: new Map(),
    deviceDidReceiveIncoming: new Map(),
    connectionDidConnect: new Map(),
    connectionIsReconnecting: new Map(),
    connectionDidReconnect: new Map(),
    connectionDidDisconnect: new Map(),
    callStateRinging: new Map(),
    callInviteCancelled: new Map(),
    callRejected: new Map(),
    audioDevicesUpdated: new Map(),
}

const Twilio = {
    // initialize the library with Twilio access token
    // return {initialized: true} when the initialization started
    // Listen to deviceReady and deviceNotReady events to see whether
    // the initialization succeeded
    async initWithToken(token) {
        if (typeof token !== 'string') {
            return {
                initialized: false,
                err:         'Invalid token, token must be a string'
            }
        };

        const result = await TwilioVoice.initWithAccessToken(token)
        // native react promise present only for Android
        // iOS initWithAccessToken doesn't return
        if (Platform.OS === IOS) {
            return {
                initialized: true,
            }
        }
        return result
    },
    refreshAccessToken(token) {
        if (typeof token !== 'string') {
            return new Error('Invalid token, token must be a string')
        }
        TwilioVoice.refreshAccessToken(token)
    },
    connect(params = {}) {
        TwilioVoice.connect(params)
    },
    disconnect: TwilioVoice.disconnect,
    accept() {
        if (Platform.OS === IOS) {
            return
        }
        TwilioVoice.accept()
    },
    reject() {
        if (Platform.OS === IOS) {
            return
        }
        TwilioVoice.reject()
    },
    ignore() {
        if (Platform.OS === IOS) {
            return
        }
        TwilioVoice.ignore()
    },
    setMuted: TwilioVoice.setMuted,
    setSpeakerPhone(value) {
        if (Platform.OS === ANDROID) {
            return
        }
        return TwilioVoice.setSpeakerPhone(value)
    },
    sendDigits: TwilioVoice.sendDigits,
    hold: TwilioVoice.setOnHold,
    requestPermissions(senderId) {
        if (Platform.OS === ANDROID) {
            TwilioVoice.requestPermissions(senderId)
        }
    },
    getActiveCall: TwilioVoice.getActiveCall,
    getCallInvite: TwilioVoice.getCallInvite,
    configureCallKit(params = {}) {
        if (Platform.OS === IOS) {
            TwilioVoice.configureCallKit(params)
        }
    },
    configureConnectionService(params = {}) {
        if (Platform.OS === ANDROID) {
            TwilioVoice.configureConnectionService(params)
        }
    },
    openPhoneAccountSetttings() {
        if (Platform.OS === ANDROID) {
            TwilioVoice.openPhoneAccountSetttings()
        }
    },
    async checkPhoneAccountEnabled() {
        return Platform.OS === IOS ? true : await TwilioVoice.checkPhoneAccountEnabled()
    },
    unregister() {
        TwilioVoice.unregister()
    },
    // getAudioDevices returns all audio devices connected
    // {
    //     "Speakerphone": false,
    //     "Earnpiece": true, // true indicates the selected audio device
    // }
    getAudioDevices() {
        if (Platform.OS === IOS) {
            return
        }
        return TwilioVoice.getAudioDevices()
    },
    // getSelectedAudioDevice returns the selected audio device
    getSelectedAudioDevice() {
        if (Platform.OS === IOS) {
            return
        }
        TwilioVoice.getSelectedAudioDevice()
    },
    // selectAudioDevice selects the passed audio device for the current active call
    selectAudioDevice(name) {
        if (Platform.OS === IOS) {
            return
        }
        TwilioVoice.selectAudioDevice(name)
    },
    addEventListener(type, handler) {
        if (!_eventHandlers.hasOwnProperty(type)) {
            throw new Error('Event handler not found: ' + type)
        }
        if (_eventHandlers[type])
        if (_eventHandlers[type].has(handler)) {
            return
        }
        _eventHandlers[type].set(handler, NativeAppEventEmitter.addListener(type, rtn => { handler(rtn) }))
    },
    removeEventListener(type, handler) {
        if (!_eventHandlers[type].has(handler)) {
            return
        }
        _eventHandlers[type].get(handler).remove()
        _eventHandlers[type].delete(handler)
    },
}

export default Twilio
