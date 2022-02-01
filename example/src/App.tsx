import React, { useEffect } from 'react'
import RNShakeDetectorModule, { Counter } from 'react-native-shake-detector'

const App = () => {
  useEffect(() => {
    console.log(RNShakeDetectorModule)

    RNShakeDetectorModule.start()
      .then(() => console.log('service started!'))
      .catch((e: Error) => console.error(e))
  })

  return <Counter />
}

export default App
