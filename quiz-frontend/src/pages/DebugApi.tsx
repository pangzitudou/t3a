import { useEffect, useState } from 'react'
import { quizApi } from '../services/quiz'

export default function DebugApi() {
  const [result, setResult] = useState<any>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const testApi = async () => {
      try {
        setLoading(true)
        console.log('=== API Test Started ===')
        console.log('Time:', new Date().toISOString())

        const response = await quizApi.getBanks()
        console.log('Raw axios response:', response)
        console.log('Response status:', response.status)
        console.log('Response data:', response.data)
        console.log('Response headers:', response.headers)

        setResult(response)
      } catch (error: any) {
        console.error('API call failed:', error)
        setResult({ error: error.message })
      } finally {
        setLoading(false)
      }
    }

    testApi()
  }, [])

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-4">API Debug Page</h1>

      <div className="mb-4">
        <button
          onClick={() => window.location.reload()}
          className="px-4 py-2 bg-blue-500 text-white rounded"
        >
          Reload Page
        </button>
      </div>

      {loading && <div className="text-blue-500">Loading...</div>}

      {result && (
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-lg font-bold mb-4">Response Object</h2>
          <pre className="bg-gray-100 p-4 rounded overflow-auto max-h-96 text-wrap">
            {JSON.stringify(result, null, 2)}
          </pre>
        </div>
      )}

      {result && result.error && (
        <div className="bg-red-100 text-red-700 p-6 rounded-lg">
          <h2 className="text-lg font-bold mb-4">Error</h2>
          <p>{result.error}</p>
        </div>
      )}

      <div className="mt-8 bg-blue-50 p-4 rounded">
        <h2 className="text-lg font-bold mb-2">How to Debug:</h2>
        <ol className="list-decimal pl-6">
          <li>Open this page in browser: http://localhost:5173/debug</li>
          <li>Press F12 to open Developer Tools</li>
          <li>Click on Console tab</li>
          <li>Look for the log messages starting with "=== API Test"</li>
          <li>Check the "Response Object" to see what data was received</li>
          <li>Check if "records" is an array with data</li>
        </ol>
      </div>
    </div>
  )
}
