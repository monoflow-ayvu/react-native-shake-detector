import {
  DeviceEventEmitter,
  EmitterSubscription,
  NativeModules,
} from 'react-native'

const RNShakeDetectorModule = NativeModules.RNShakeDetectorModule

export function start(
  maxSamples = 25,
  minTimeBetweenSamplesMs = 20,
  visibleTimeRangeMs = 500,
  magnitudeThreshold = 25,
  percentOverThresholdForShake = 66,
  useAudioClassifier = false
): Promise<boolean> {
  return RNShakeDetectorModule.start(
    maxSamples,
    minTimeBetweenSamplesMs,
    visibleTimeRangeMs,
    magnitudeThreshold,
    percentOverThresholdForShake,
    useAudioClassifier
  )
}

export function stop(): Promise<boolean> {
  return RNShakeDetectorModule.stop()
}

export function onShake(
  callback: (ev: {
    percentOverThreshold: number
    classifications: Record<string, number>
  }) => void
): EmitterSubscription {
  return DeviceEventEmitter.addListener('shake', callback)
}

export function classify(): Promise<void> {
  return RNShakeDetectorModule.classify()
}
