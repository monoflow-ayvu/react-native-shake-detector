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
  percentOverThresholdForShake = 66
): Promise<boolean> {
  return RNShakeDetectorModule.start(
    maxSamples,
    minTimeBetweenSamplesMs,
    visibleTimeRangeMs,
    magnitudeThreshold,
    percentOverThresholdForShake
  )
}

export function stop(): Promise<boolean> {
  return RNShakeDetectorModule.stop()
}

export function onShake(
  callback: (ev: { percentOverThreshold: number }) => void
): EmitterSubscription {
  return DeviceEventEmitter.addListener('shake', callback)
}
