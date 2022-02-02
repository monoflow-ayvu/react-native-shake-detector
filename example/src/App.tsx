import React, { useEffect } from 'react'
import { DeviceEventEmitter } from 'react-native'
import RNShakeDetectorModule, { Counter } from 'react-native-shake-detector'

function start(
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

const App = () => {
  useEffect(() => {
    console.log(RNShakeDetectorModule)

    DeviceEventEmitter.addListener('shake', (ev) => {
      console.info('shake', ev)
    })

    start(50, 1000 * 5, 500, 25, 66)
      .then(() => console.log('service started!'))
      .catch((e: Error) => console.error(e))
  })

  return <Counter />
}

export default App
