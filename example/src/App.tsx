/* eslint-disable react-native/no-inline-styles */
import { Buffer } from 'buffer'
import {
  Actionsheet,
  Box,
  Button,
  Center,
  Divider,
  FormControl,
  Heading,
  HStack,
  NativeBaseProvider,
  ScrollView,
  Slider,
  Stack,
  Text,
  useDisclose,
  View,
  VStack,
} from 'native-base'
import React, { useEffect } from 'react'
import { Alert, PermissionsAndroid } from 'react-native'
import * as Shaker from 'react-native-shake-detector'
import Share from 'react-native-share'

type Config = {
  maxSamples: number
  minTimeBetweenSamplesMs: number
  visibleTimeRangeMs: number
  magnitudeThreshold: number
  percentOverThresholdForShake: number
  useAudioClassifier: boolean
}

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

function Configure({
  config,
  setConfig,
}: {
  config: Config
  setConfig: React.Dispatch<React.SetStateAction<Config>>
}) {
  const { isOpen, onOpen, onClose } = useDisclose()

  return (
    <Center>
      <Button onPress={() => onOpen()}>Configurar</Button>
      <Actionsheet isOpen={isOpen} onClose={onClose}>
        <Actionsheet.Content>
          <ScrollView>
            <PropInput
              value={config.maxSamples}
              cb={(val) => setConfig((c) => ({ ...c, maxSamples: val }))}
              name='Cantidad máxima de muestreos'
              min={1}
              max={500}
              step={1}
              help='La cantidad máxima de muestreos que se guardan en el histórico para calcular colisiones'
            />
            <PropInput
              value={config.minTimeBetweenSamplesMs}
              cb={(val) =>
                setConfig((c) => ({ ...c, minTimeBetweenSamplesMs: val }))
              }
              name='Tiempo mínimo entre muestreos (milisegundos)'
              min={10}
              max={20 * 1000}
              step={10}
              help='Tiempo mínimo a esperar para tomar un nuevo valor del acelerómetro'
            />
            <PropInput
              value={config.visibleTimeRangeMs}
              cb={(val) =>
                setConfig((c) => ({ ...c, visibleTimeRangeMs: val }))
              }
              name='Rango de tiempo visible (milisegundos)'
              min={10}
              max={5 * 1000}
              step={10}
              help='Tiempo máximo a considerar un valor como parte del histórico (si se supera, el valor es ignorado al calcular colisión)'
            />
            <PropInput
              value={config.magnitudeThreshold}
              cb={(val) =>
                setConfig((c) => ({ ...c, magnitudeThreshold: val }))
              }
              name='Magnitud mínima'
              min={1}
              max={200}
              step={1}
              help='Nivel mínimo de aceleración (fuerza G) para considerar una colisión'
            />
            <PropInput
              value={config.percentOverThresholdForShake}
              cb={(val) =>
                setConfig((c) => ({ ...c, percentOverThresholdForShake: val }))
              }
              name='Porcentaje sobre media de magnitud para impacto'
              help='Porcentaje mínimo del total de muestreos que tienen que superar la magnitud mínima para considerar un impacto'
            />
          </ScrollView>
        </Actionsheet.Content>
      </Actionsheet>
    </Center>
  )
}

function LogItem({
  item,
}: {
  item: {
    at: Date
    percentOverThreshold: number
    classifications: Record<string, number>
  }
}) {
  return (
    <Box width='90%' alignItems='stretch'>
      <Box
        rounded='lg'
        overflow='hidden'
        borderColor='coolGray.200'
        borderWidth='1'
        _dark={{
          borderColor: 'coolGray.600',
          backgroundColor: 'gray.700',
        }}
        _web={{
          shadow: 2,
          borderWidth: 0,
        }}
        _light={{
          backgroundColor: 'gray.50',
        }}
      >
        <Stack p='4' space={3}>
          <Stack space={2}>
            <Heading size='md' ml='-1'>
              {item.at.toLocaleTimeString()}
            </Heading>
            <Text
              fontSize='xs'
              _light={{
                color: 'violet.500',
              }}
              _dark={{
                color: 'violet.400',
              }}
              fontWeight='500'
              ml='-0.5'
              mt='-1'
            >
              Impacto de: {item.percentOverThreshold.toFixed(2)}%
            </Text>
          </Stack>
          <Text fontWeight='400'>
            {Object.keys(item.classifications).map((v, i) => (
              <Text key={i}>
                {v}: {(item.classifications[v] * 100).toFixed(1)}% {'\n'}
              </Text>
            ))}
          </Text>
          <HStack alignItems='center' space={4} justifyContent='space-between'>
            <HStack alignItems='center'>
              <Text
                color='coolGray.600'
                _dark={{
                  color: 'warmGray.200',
                }}
                fontWeight='400'
              >
                {item.at.toISOString()}
              </Text>
            </HStack>
          </HStack>
        </Stack>
      </Box>
    </Box>
  )
}

const App = () => {
  const [loading, setLoading] = React.useState(true)
  const [collisions, setCollisions] = React.useState<
    {
      at: Date
      percentOverThreshold: number
      classifications: Record<string, number>
    }[]
  >([])
  const [config, setConfig] = React.useState({
    maxSamples: 25,
    minTimeBetweenSamplesMs: 1000 * 5,
    visibleTimeRangeMs: 500,
    magnitudeThreshold: 25,
    percentOverThresholdForShake: 66,
    useAudioClassifier: true,
  })

  useEffect(() => {
    const listener = Shaker.onShake((ev) => {
      setCollisions((c) => [
        ...c,
        {
          at: new Date(),
          percentOverThreshold: ev.percentOverThreshold * 100,
          classifications: Object.keys(ev.classifications)
            .sort((a, b) => ev.classifications[b] - ev.classifications[a])
            .reduce((acc, v) => ({ ...acc, [v]: ev.classifications[v] }), {}),
        },
      ])
    })
    setLoading(true)

    PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.RECORD_AUDIO)
      .then((granted) => {
        if (granted !== 'granted') {
          throw new Error('Permission denied')
        }
      })
      .then(() =>
        Shaker.start(
          config.maxSamples,
          config.minTimeBetweenSamplesMs,
          config.visibleTimeRangeMs,
          config.magnitudeThreshold,
          config.percentOverThresholdForShake,
          config.useAudioClassifier
        )
      )
      .then(() => setLoading(false))
      .catch((e: Error) => Alert.alert(String(e)))

    return () => {
      listener.remove()
      Shaker.stop().catch((e: Error) => console.error(e))
    }
  }, [config])

  const orderedCollisions = React.useMemo(
    () =>
      collisions.sort((a, b) => {
        if (a.at > b.at) {
          return -1
        }
        if (a.at < b.at) {
          return 1
        }
        return 0
      }),
    [collisions]
  )

  const lastCollision = React.useMemo(() => {
    if (orderedCollisions.length === 0) {
      return null
    }
    // return max by time
    return orderedCollisions[0]
  }, [orderedCollisions])

  const save = React.useCallback(async () => {
    const csvData = Object.keys(orderedCollisions)
      .map((v, i) => {
        const item = orderedCollisions[i]
        return [
          item.at.toISOString(),
          item.percentOverThreshold.toFixed(2),
          ...Object.keys(item.classifications).map(
            (k) => `"${k}=${item.classifications[k].toFixed(2)}"`
          ),
        ].join(',')
      })
      .join('\n')

    Share.open({
      title: 'ImpactAI ' + new Date().toISOString(),
      url: 'data:text/csv;base64,' + Buffer.from(csvData).toString('base64'),
      type: 'text/csv',
      showAppsToView: true,
      filename: 'ImpactAI ' + new Date().toISOString(),
    }).catch((_e: Error) => {
      /* ignore */
    })

    console.info(csvData)
  }, [orderedCollisions])

  return (
    <NativeBaseProvider>
      <ScrollView contentContainerStyle={{ paddingHorizontal: '8%' }}>
        <View style={{ height: 30 }} />

        <Box alignSelf='center'>
          <Heading size='2xl' textAlign='center'>
            {loading ? 'Cargando...' : 'ImpactAI'}
          </Heading>
          {lastCollision && (
            <>
              <Heading size='sm' textAlign='center'>
                {lastCollision.at.toISOString()}
              </Heading>
              <Heading size='md' textAlign='center'>
                {`${lastCollision.percentOverThreshold.toFixed(2)}%`}
              </Heading>
              {Object.keys(lastCollision.classifications).map((key) => (
                <Heading size='sm' textAlign='center' key={key}>
                  {`${key}: ${(
                    lastCollision.classifications[key] * 100
                  ).toFixed(1)}%`}
                </Heading>
              ))}
            </>
          )}
        </Box>

        <View
          style={{
            height: 30,
          }}
        />

        <HStack space={3} justifyContent='center'>
          <Configure config={config} setConfig={setConfig} />
          <Button onPress={save}>Salvar CSV</Button>
        </HStack>

        <View style={{ height: 30 }} />

        <VStack space={4} alignItems='center'>
          {orderedCollisions.map((c) => (
            <LogItem key={c.at.toISOString()} item={c} />
          ))}
        </VStack>

        <View style={{ height: 30 }} />
      </ScrollView>
    </NativeBaseProvider>
  )
}

// const styles = StyleSheet.create({})

export default App
