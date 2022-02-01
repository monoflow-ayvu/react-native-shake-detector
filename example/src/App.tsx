import React, { useEffect } from 'react'
import RNShakeDetectorModule, { Counter } from 'react-native-shake-detector'

const App = () => {
  useEffect(() => {
    console.log(RNShakeDetectorModule)
  })

  return <Counter />
}

export default App
