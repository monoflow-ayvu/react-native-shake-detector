import {
  Box,
  Divider,
  FormControl,
  Heading,
  Input,
  NativeBaseProvider,
} from 'native-base'
import React, { useEffect } from 'react'
import { DeviceEventEmitter, Vibration } from 'react-native'
import RNShakeDetectorModule from 'react-native-shake-detector'

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

function PropInput({
  name,
  value,
  cb,
}: {
  value: number
  name: string
  cb: (val: number) => void
}) {
  const onChangeNumber = React.useCallback(
    (val: string) => {
      cb(Number(val))
    },
    [cb]
  )

  return (
    <Box>
      <FormControl mb='5'>
        <FormControl.Label>{name}</FormControl.Label>
        <Input value={value.toString()} onChangeText={onChangeNumber} />
        {/* <FormControl.HelperText>
          Give your project a title.
        </FormControl.HelperText> */}
      </FormControl>
      <Divider />
    </Box>
  )
}

const App = () => {
  const [magnitude, setMagnitude] = React.useState(0)
  const [maxSamples, setMaxSamples] = React.useState(50)
  const [minTimeBetweenSamplesMs, setMinTimeBetweenSamplesMs] = React.useState(
    1000 * 5
  )
  const [visibleTimeRangeMs, setVisibleTimeRangeMs] = React.useState(500)
  const [magnitudeThreshold, setMagnitudeThreshold] = React.useState(25)
  const [percentOverThresholdForShake, setPercentOverThresholdForShake] =
    React.useState(66)

  useEffect(() => {
    console.log(RNShakeDetectorModule)

    const listener = DeviceEventEmitter.addListener('shake', (ev) => {
      console.info('shake', ev)
      setMagnitude(ev.percentOverThreshold * 100)
      Vibration.vibrate(10)
    })

    start(
      maxSamples,
      minTimeBetweenSamplesMs,
      visibleTimeRangeMs,
      magnitudeThreshold,
      percentOverThresholdForShake
    )
      .then(() => console.log('service started!'))
      .catch((e: Error) => console.error(e))

    return () => {
      listener.remove()
      RNShakeDetectorModule.stop().catch((e: Error) => console.error(e))
    }
  }, [
    magnitudeThreshold,
    maxSamples,
    minTimeBetweenSamplesMs,
    percentOverThresholdForShake,
    setMagnitude,
    visibleTimeRangeMs,
  ])

  return (
    <NativeBaseProvider>
      <Box>
        <PropInput
          value={maxSamples}
          cb={setMaxSamples}
          name='Cantidad máxima de muestreos'
        />
        <PropInput
          value={minTimeBetweenSamplesMs}
          cb={setMinTimeBetweenSamplesMs}
          name='Tiempo mínimo entre muestreos (milisegundos)'
        />
        <PropInput
          value={visibleTimeRangeMs}
          cb={setVisibleTimeRangeMs}
          name='Rango de tiempo visible (milisegundos)'
        />
        <PropInput
          value={magnitudeThreshold}
          cb={setMagnitudeThreshold}
          name='Magnitud mínima'
        />
        <PropInput
          value={percentOverThresholdForShake}
          cb={setPercentOverThresholdForShake}
          name='Porcentaje sobre media de magnitud para impacto'
        />

        <Box alignSelf='center'>
          <Heading>Porcentaje sobre la línea:</Heading>
          <Heading size='4xl' textAlign='center'>
            {magnitude.toFixed(0)}%
          </Heading>
        </Box>
      </Box>
    </NativeBaseProvider>
  )
}

// const styles = StyleSheet.create({})

export default App
