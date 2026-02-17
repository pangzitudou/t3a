import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'

export default function HomePage() {
  const navigate = useNavigate()

  const features = [
    {
      icon: '🤖',
      title: 'AI-Generated Question Banks',
      description: 'Upload your learning materials and let AI create personalized quizzes',
    },
    {
      icon: '✓',
      title: 'Intelligent Auto-Grading',
      description: 'Get instant feedback on objective questions and AI-powered subjective scoring',
    },
    {
      icon: '📊',
      title: 'Knowledge Gap Analysis',
      description: 'Identify your strengths and weaknesses with detailed AI insights',
    },
    {
      icon: '📡',
      title: 'Real-time Progress Sync',
      description: 'WebSocket-powered live updates during quiz sessions',
    },
  ]

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <motion.section
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="min-h-screen flex items-center justify-center px-4"
      >
        <div className="text-center max-w-4xl">
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ type: 'spring', delay: 0.2 }}
            className="text-8xl mb-6"
          >
            🎓
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="text-6xl md:text-7xl font-bold text-white mb-6"
          >
            TestAgainAndAgain
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4 }}
            className="text-2xl md:text-3xl text-white/90 mb-8"
          >
            AI-Powered Quiz Platform for Smarter Learning
          </motion.p>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
            className="text-lg text-white/80 mb-12 max-w-2xl mx-auto"
          >
            Transform your study materials into interactive quizzes. Get instant feedback,
            personalized analysis, and track your progress over time.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.6 }}
            className="flex flex-col sm:flex-row gap-4 justify-center"
          >
            <Button onClick={() => navigate('/register')} size="lg">
              Get Started Free
            </Button>
            <Button onClick={() => navigate('/login')} variant="outline" size="lg">
              Sign In
            </Button>
          </motion.div>

          {/* Tech Stack Badges */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.8 }}
            className="flex flex-wrap items-center justify-center gap-3 mt-12"
          >
            {[
              { label: 'Spring AI', color: 'bg-green-500' },
              { label: 'DeepSeek-V3', color: 'bg-blue-500' },
              { label: 'React 19', color: 'bg-cyan-500' },
              { label: 'WebSocket', color: 'bg-purple-500' },
            ].map((tech, index) => (
              <span
                key={index}
                className={`px-4 py-2 ${tech.color} text-white rounded-full text-sm font-semibold shadow-lg`}
              >
                {tech.label}
              </span>
            ))}
          </motion.div>
        </div>
      </motion.section>

      {/* Features Section */}
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-4xl md:text-5xl font-bold text-white mb-4">
              Everything You Need to Learn Smarter
            </h2>
            <p className="text-xl text-white/80">
              Powered by cutting-edge AI technology
            </p>
          </motion.div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: index * 0.1 }}
              >
                <Card hover className="h-full">
                  <div className="text-5xl mb-4">{feature.icon}</div>
                  <h3 className="text-xl font-bold text-gray-800 mb-2">
                    {feature.title}
                  </h3>
                  <p className="text-gray-600">{feature.description}</p>
                </Card>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section className="py-20 px-4">
        <div className="max-w-5xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-4xl md:text-5xl font-bold text-white mb-4">
              How It Works
            </h2>
            <p className="text-xl text-white/80">
              Four simple steps to smarter learning
            </p>
          </motion.div>

          <div className="space-y-6">
            {[
              {
                step: '01',
                title: 'Upload Your Materials',
                description: 'Upload PDF, TXT, or DOCX files containing your study content',
                icon: '📄',
              },
              {
                step: '02',
                title: 'AI Generates Questions',
                description: 'Our AI analyzes your content and creates various question types',
                icon: '🤖',
              },
              {
                step: '03',
                title: 'Take the Quiz',
                description: 'Answer questions with real-time progress tracking and instant feedback',
                icon: '✏️',
              },
              {
                step: '04',
                title: 'Get AI Analysis',
                description: 'Receive detailed insights on your knowledge gaps and personalized recommendations',
                icon: '📊',
              },
            ].map((item, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, x: -50 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ delay: index * 0.1 }}
                className="flex items-start gap-6"
              >
                <div className="flex-shrink-0 w-16 h-16 bg-gradient-to-br from-primary-500 to-secondary-500 rounded-2xl flex items-center justify-center text-white text-xl font-bold shadow-lg">
                  {item.step}
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="text-3xl">{item.icon}</span>
                    <h3 className="text-2xl font-bold text-white">{item.title}</h3>
                  </div>
                  <p className="text-white/80 text-lg">{item.description}</p>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="max-w-4xl mx-auto text-center"
        >
          <Card className="py-16 px-8">
            <h2 className="text-4xl font-bold text-gray-800 mb-4">
              Ready to Transform Your Learning?
            </h2>
            <p className="text-xl text-gray-600 mb-8">
              Join thousands of students learning smarter with AI-powered quizzes
            </p>
            <Button onClick={() => navigate('/register')} size="lg">
              Start Learning Now — It's Free
            </Button>
          </Card>
        </motion.div>
      </section>

      {/* Footer */}
      <footer className="py-8 px-4 text-center text-white/80">
        <p className="mb-2">
          © 2026 TestAgainAndAgain (T3A) - AI-Powered Quiz Platform
        </p>
        <p className="text-sm">
          Built with Spring AI • DeepSeek-V3 • React 19 • WebSocket
        </p>
      </footer>
    </div>
  )
}
