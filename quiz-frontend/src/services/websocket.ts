export type WebSocketMessage =
  | { type: 'progress'; data: { current: number; total: number } }
  | { type: 'time_warning'; data: { remaining: number } }
  | { type: 'answer_saved'; data: { questionId: string } }
  | { type: 'completion'; data: { sessionKey: string } }
  | { type: 'error'; data: { message: string } }

export type WebSocketEventHandler = (message: WebSocketMessage) => void

export class QuizWebSocket {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000
  private handlers: Set<WebSocketEventHandler> = new Set()
  private url: string

  constructor(sessionKey: string) {
    this.url = `ws://localhost:8083/quiz/${sessionKey}`
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.url)

        this.ws.onopen = () => {
          console.log('WebSocket connected')
          this.reconnectAttempts = 0
          resolve()
        }

        this.ws.onmessage = (event) => {
          try {
            const message: WebSocketMessage = JSON.parse(event.data)
            this.handlers.forEach((handler) => handler(message))
          } catch (error) {
            console.error('Failed to parse WebSocket message:', error)
          }
        }

        this.ws.onerror = (error) => {
          console.error('WebSocket error:', error)
          reject(error)
        }

        this.ws.onclose = () => {
          console.log('WebSocket disconnected')
          this.attemptReconnect()
        }
      } catch (error) {
        reject(error)
      }
    })
  }

  private attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      const delay = this.reconnectDelay * this.reconnectAttempts

      console.log(`Attempting to reconnect in ${delay}ms...`)

      setTimeout(() => {
        this.connect().catch((error) => {
          console.error('Reconnection failed:', error)
        })
      }, delay)
    }
  }

  onMessage(handler: WebSocketEventHandler) {
    this.handlers.add(handler)
    return () => {
      this.handlers.delete(handler)
    }
  }

  send(data: any) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    } else {
      console.warn('WebSocket is not connected')
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.handlers.clear()
  }

  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN
  }
}

// Singleton instance for the current session
let currentWS: QuizWebSocket | null = null

export const connectQuizWebSocket = (sessionKey: string) => {
  if (currentWS) {
    currentWS.disconnect()
  }
  currentWS = new QuizWebSocket(sessionKey)
  return currentWS
}

export const disconnectQuizWebSocket = () => {
  if (currentWS) {
    currentWS.disconnect()
    currentWS = null
  }
}

export const getQuizWebSocket = () => currentWS
