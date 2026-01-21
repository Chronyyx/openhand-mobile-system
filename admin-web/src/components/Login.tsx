import React, { useState } from 'react'
import axios from 'axios'
import { Lock, Mail } from 'lucide-react'

interface LoginProps {
    setToken: (token: string) => void
}

const Login: React.FC<LoginProps> = ({ setToken }) => {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setLoading(true)
        setError('')
        try {
            const response = await axios.post('/api/auth/login', { email: email, password })
            // The backend returns { token: "..." } not accessToken
            if (response.data.token) {
                setToken(response.data.token)
            } else {
                setError('Invalid response from server')
            }
        } catch (err) {
            setError('Login failed. Please check your credentials.')
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-100">
            <div className="px-8 py-6 mt-4 text-left bg-white shadow-lg rounded-xl max-w-md w-full border border-gray-200">
                <div className="flex justify-center mb-4">
                    <div className="p-3 bg-blue-100 rounded-full">
                        <Lock className="w-8 h-8 text-blue-600" />
                    </div>
                </div>
                <h3 className="text-2xl font-bold text-center text-gray-800">CI/CD WORKS!</h3>
                <p className="mt-2 text-center text-sm text-gray-500">Access the secure audit logs</p>

                <form onSubmit={handleSubmit} className="mt-6 space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Email</label>
                        <div className="relative mt-1">
                            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                <Mail className="h-5 w-5 text-gray-400" />
                            </div>
                            <input
                                type="text"
                                placeholder="admin@example.com"
                                className="w-full pl-10 px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                            />
                        </div>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Password</label>
                        <div className="relative mt-1">
                            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                <Lock className="h-5 w-5 text-gray-400" />
                            </div>
                            <input
                                type="password"
                                placeholder="••••••••"
                                className="w-full pl-10 px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                            />
                        </div>
                    </div>
                    {error && <div className="text-sm text-red-600 bg-red-50 p-2 rounded border border-red-200">{error}</div>}
                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full px-6 py-2.5 text-white bg-blue-600 rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50 transition-colors font-medium shadow-sm disabled:opacity-70 disabled:cursor-not-allowed"
                    >
                        {loading ? 'Signing in...' : 'Sign In'}
                    </button>
                </form>
            </div>
        </div>
    )
}

export default Login
