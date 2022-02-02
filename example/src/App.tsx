import {
  Box,
  Button,
  Divider,
  FormControl,
  Heading,
  NativeBaseProvider,
  ScrollView,
  Slider,
  View,
} from 'native-base'
import React, { useEffect } from 'react'
import * as Shaker from 'react-native-shake-detector'
const Sparkline: any = require('react-native-sparkline').default

function PropInput({
  name,
  value,
  cb,
  min = 0,
  max = 100,
  step = 10,
  help = '',
}: {
  value: number
  name: string
  cb: (val: number) => void
  min?: number
  max?: number
  step?: number
  help?: string
}) {
  const onChangeNumber = React.useCallback(
    (val: string | number) => {
      cb(Number(val))
    },
    [cb]
  )

  return (
    <Box>
      <FormControl mb='5'>
        <FormControl.Label>{name}</FormControl.Label>
        <Slider
          defaultValue={value}
          onChangeEnd={onChangeNumber}
          minValue={min}
          maxValue={max}
          step={step}
        >
          <Slider.Track>
            <Slider.FilledTrack />
          </Slider.Track>
          <Slider.Thumb />
        </Slider>
        {/* <Input value={value.toString()} onChangeText={onChangeNumber} /> */}
        <FormControl.HelperText alignSelf='center'>
          {help ? help + '\n' : ''}
          {`Valor actual: ${value}`}
        </FormControl.HelperText>
      </FormControl>
      <Divider />
    </Box>
  )
}

const average = (arr: number[]) => arr.reduce((p, c) => p + c, 0) / arr.length

const App = () => {
  const [loading, setLoading] = React.useState(true)
  const [maxSamples, setMaxSamples] = React.useState(50)
  const [minTimeBetweenSamplesMs, setMinTimeBetweenSamplesMs] = React.useState(
    1000 * 5
  )
  const [visibleTimeRangeMs, setVisibleTimeRangeMs] = React.useState(500)
  const [magnitudeThreshold, setMagnitudeThreshold] = React.useState(25)
  const [percentOverThresholdForShake, setPercentOverThresholdForShake] =
    React.useState(66)
  const [magnitudes, setMagnitudes] = React.useState<number[]>(
    new Array<number>(maxSamples).fill(0)
  )

  const onMagnitude = React.useCallback(
    (val: number) => {
      setMagnitudes((old) => [...old, val].slice(-maxSamples))
    },
    [maxSamples]
  )

  useEffect(() => {
    const listener = Shaker.onShake((ev) => {
      onMagnitude(ev.percentOverThreshold * 100)
    })
    setLoading(true)

    Shaker.start(
      maxSamples,
      minTimeBetweenSamplesMs,
      visibleTimeRangeMs,
      magnitudeThreshold,
      percentOverThresholdForShake
    )
      .then(() => setLoading(false))
      .catch((e: Error) => console.error(e))

    return () => {
      listener.remove()
      Shaker.stop().catch((e: Error) => console.error(e))
    }
  }, [
    magnitudeThreshold,
    maxSamples,
    minTimeBetweenSamplesMs,
    onMagnitude,
    percentOverThresholdForShake,
    setMagnitudes,
    visibleTimeRangeMs,
  ])

  return (
    <NativeBaseProvider>
      <ScrollView contentContainerStyle={{ paddingHorizontal: '8%' }}>
        <View style={{ height: 30 }} />

        <Box>
          <PropInput
            value={maxSamples}
            cb={setMaxSamples}
            name='Cantidad máxima de muestreos'
            min={1}
            max={500}
            step={1}
            help='La cantidad máxima de muestreos que se guardan en el histórico para calcular colisiones'
          />
          <PropInput
            value={minTimeBetweenSamplesMs}
            cb={setMinTimeBetweenSamplesMs}
            name='Tiempo mínimo entre muestreos (milisegundos)'
            min={10}
            max={20 * 1000}
            step={10}
            help='Tiempo mínimo a esperar para tomar un nuevo valor del acelerómetro'
          />
          <PropInput
            value={visibleTimeRangeMs}
            cb={setVisibleTimeRangeMs}
            name='Rango de tiempo visible (milisegundos)'
            min={10}
            max={5 * 1000}
            step={10}
            help='Tiempo máximo a considerar un valor como parte del histórico (si se supera, el valor es ignorado al calcular colisión)'
          />
          <PropInput
            value={magnitudeThreshold}
            cb={setMagnitudeThreshold}
            name='Magnitud mínima'
            min={1}
            max={200}
            step={1}
            help='Nivel mínimo de aceleración (fuerza G) para considerar una colisión'
          />
          <PropInput
            value={percentOverThresholdForShake}
            cb={setPercentOverThresholdForShake}
            name='Porcentaje sobre media de magnitud para impacto'
            help='Porcentaje mínimo del total de muestreos que tienen que superar la magnitud mínima para considerar un impacto'
          />

          <Box alignSelf='center'>
            <Heading size='2xl'>
              {loading ? 'Cargando...' : 'Impactos detectados'}
            </Heading>
            <Box alignSelf='center' alignContent='center' alignItems='center'>
              <Sparkline data={magnitudes}>
                <Sparkline.Line />
                {/* <Sparkline.Spots /> */}
                {/* <Sparkline.Fill /> */}
              </Sparkline>
            </Box>
            <Heading size='sm' textAlign='center'>
              (% sobre media de magnitud)
            </Heading>

            <View style={{ height: 30 }} />
            <Heading textAlign='center'>
              Total: {magnitudes.filter((m) => m > 0).length}
            </Heading>
            <Heading textAlign='center'>
              Max: {Math.max(...magnitudes).toFixed(0)}%
            </Heading>
            <Heading textAlign='center'>
              Avg: {average(magnitudes).toFixed(0)}%
            </Heading>
            <Heading textAlign='center'>
              Min: {Math.min(...magnitudes).toFixed(0)}%
            </Heading>
          </Box>
        </Box>

        <View style={{ height: 30 }} />
        <Box alignItems='center'>
          <Button
            onPress={() => setMagnitudes(new Array<number>(maxSamples).fill(0))}
          >
            Reset
          </Button>
        </Box>

        <View style={{ height: 100 }} />
      </ScrollView>
    </NativeBaseProvider>
  )
}

// const styles = StyleSheet.create({})

export default App
