import React, { useEffect, useState, useRef, useCallback } from 'react'
import axios from 'axios'
import { Client } from '@stomp/stompjs'
import { Search, Download, LogOut, RefreshCw, ChevronLeft, ChevronRight } from 'lucide-react'

interface AuditLog {
    id: number
    userId: number
    affectedUserEmail: string
    previousRole: string
    newRole: string
    changedBy: string
    changedAt: string
    ipAddress: string
    userAgent: string
    source: string
}

interface AuditLogTableProps {
    token: string
    logout: () => void
}

interface FetchParams {
    page: number
    size: number
    sort: string
    type: string
    skipLog: boolean
    search?: string
    from?: string
    to?: string
}

const AuditLogTable: React.FC<AuditLogTableProps> = ({ token, logout }) => {
    const [logs, setLogs] = useState<AuditLog[]>([])
    const [loading, setLoading] = useState(false)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)

    // State for UI binding
    const [search, setSearch] = useState('')
    const [activeTab, setActiveTab] = useState('CHANGES') // 'CHANGES' or 'ACCESS'
    const [fromDate, setFromDate] = useState('')
    const [toDate, setToDate] = useState('')

    // Refs for stable access in fetchLogs (solves closure staleness)
    const filtersRef = useRef({ search, activeTab, fromDate, toDate }) // Initial values

    // Keep refs in sync with state
    useEffect(() => {
        filtersRef.current = { search, activeTab, fromDate, toDate }
    }, [search, activeTab, fromDate, toDate])

    // WebSocket ref
    const stompClient = useRef<Client | null>(null)

    const fetchLogs = useCallback(async (currentPage = 0, skipLogging = false) => {
        setLoading(true)
        try {
            const { search, activeTab, fromDate, toDate } = filtersRef.current

            const params: FetchParams = {
                page: currentPage,
                size: 10,
                sort: 'changedAt,desc',
                type: activeTab,
                skipLog: skipLogging
            }
            if (search) params.search = search
            if (fromDate) params.from = fromDate
            if (toDate) params.to = toDate

            const response = await axios.get('/api/admin/audit-logs', {
                headers: { Authorization: `Bearer ${token}` },
                params
            })
            setLogs(response.data.content)
            setTotalPages(response.data.totalPages)
            setPage(currentPage)
        } catch (err) {
            console.error('Failed to fetch logs', err)
            if (axios.isAxiosError(err) && err.response?.status === 401) {
                logout()
            }
        } finally {
            setLoading(false)
        }
    }, [token, logout])

    // Effect for activeTab changes (New View -> Log it)
    useEffect(() => {
        fetchLogs(0, false)
    }, [activeTab, fetchLogs])

    // Effect for date changes (Filtering -> do not log view again? or should we? User said "one time")
    // Let's assume date changes are refinements, so skip log.
    useEffect(() => {
        if (fromDate || toDate) fetchLogs(0, true)
    }, [fromDate, toDate, fetchLogs])

    // Initial load handled by activeTab effect

    useEffect(() => {
        // Setup WebSocket
        const client = new Client({
            // Vite Proxy: /ws -> ws://backend:8080. Browser connects to ws://localhost:3000/ws
            brokerURL: `ws://${window.location.host}/ws`,
            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },
            onConnect: () => {
                client.subscribe('/topic/audit-logs', () => {
                    // Real-time update: simple refresh, skip log
                    // Thanks to filtersRef and useCallback, this correctly uses current filters!
                    fetchLogs(0, true)
                })
            },
            debug: (str) => {
                console.log(str);
            }
        })

        client.activate()
        stompClient.current = client

        return () => {
            client.deactivate()
        }
    }, [token, fetchLogs]) // Re-connect if token changes

    const handleExport = async () => {
        try {
            const { search, activeTab, fromDate, toDate } = filtersRef.current

            const params: Partial<FetchParams> = { type: activeTab }
            if (search) params.search = search
            if (fromDate) params.from = fromDate
            if (toDate) params.to = toDate

            const response = await axios.get('/api/admin/audit-logs/export', {
                headers: { Authorization: `Bearer ${token}` },
                params,
                responseType: 'blob',
            })

            const url = window.URL.createObjectURL(new Blob([response.data]))
            const link = document.createElement('a')
            link.href = url
            link.setAttribute('download', 'audit-logs.csv')
            document.body.appendChild(link)
            link.click()
            link.remove()
        } catch (err) {
            console.error("Export failed", err)
        }
    }

    return (
        <div className="p-8 max-w-7xl mx-auto">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-gray-900">Audit Logs</h1>
                    <p className="text-gray-500 mt-1">Track system changes and security events</p>
                </div>
                <div className="flex gap-4">
                    <button onClick={() => fetchLogs(page, true)} className="p-2 text-gray-600 hover:text-blue-600 transition-colors" title="Refresh">
                        <RefreshCw className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} />
                    </button>
                    <button
                        onClick={handleExport}
                        className="flex items-center gap-2 bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors shadow-sm font-medium"
                    >
                        <Download className="w-4 h-4" /> Export CSV
                    </button>
                    <button
                        onClick={logout}
                        className="flex items-center gap-2 bg-white text-red-600 border border-red-200 px-4 py-2 rounded-lg hover:bg-red-50 transition-colors shadow-sm font-medium"
                    >
                        <LogOut className="w-4 h-4" /> Logout
                    </button>
                </div>
            </div>

            {/* Filters */}
            <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-200 mb-6 flex flex-wrap gap-4 items-end">
                <div className="flex-1 min-w-[200px]">
                    <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Search</label>
                    <div className="relative">
                        <Search className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
                        <input
                            type="text"
                            placeholder={activeTab === 'ACCESS' ? "Search activity..." : "User, Role, or Admin..."}
                            className="pl-9 w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && fetchLogs(0, false)} // Log search
                        />
                    </div>
                </div>
                <div>
                    <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">From Date</label>
                    <input
                        type="date"
                        className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                        value={fromDate}
                        onChange={(e) => setFromDate(e.target.value)}
                    />
                </div>
                <div>
                    <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">To Date</label>
                    <input
                        type="date"
                        className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
                        value={toDate}
                        onChange={(e) => setToDate(e.target.value)}
                    />
                </div>
            </div>

            {/* Tabs */}
            <div className="flex border-b border-gray-200 mb-6">
                <button
                    className={`px-6 py-3 text-sm font-medium border-b-2 transition-colors ${activeTab === 'CHANGES' ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
                    onClick={() => {
                        setPage(0)
                        setLogs([])
                        setActiveTab('CHANGES')
                    }}
                >
                    User Changes
                </button>
                <button
                    className={`px-6 py-3 text-sm font-medium border-b-2 transition-colors ${activeTab === 'ACCESS' ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
                    onClick={() => {
                        setPage(0)
                        setLogs([])
                        setActiveTab('ACCESS')
                    }}
                >
                    Admin Access Logs
                </button>
            </div>

            {/* Table */}
            <div className="bg-white shadow-sm rounded-xl border border-gray-200 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-gray-50 border-b border-gray-200 text-xs uppercase text-gray-500 font-semibold tracking-wider">
                                <th className="px-6 py-4">Timestamp</th>
                                <th className="px-6 py-4">{activeTab === 'ACCESS' ? 'Viewed By' : 'Changed By'}</th>
                                <th className="px-6 py-4">{activeTab === 'ACCESS' ? 'Log Type' : 'Affected User'}</th>
                                {activeTab === 'CHANGES' && <th className="px-6 py-4">Change</th>}
                                <th className="px-6 py-4">Source</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {logs.length === 0 ? (
                                <tr>
                                    <td colSpan={activeTab === 'CHANGES' ? 5 : 4} className="px-6 py-8 text-center text-gray-500 italic">
                                        No logs found matching your criteria.
                                    </td>
                                </tr>
                            ) : (
                                logs.map((log) => (
                                    <tr key={log.id} className="hover:bg-gray-50/50 transition-colors">
                                        <td className="px-6 py-4 text-sm text-gray-600 whitespace-nowrap font-mono">{new Date(log.changedAt.endsWith('Z') ? log.changedAt : log.changedAt + 'Z').toLocaleString()}</td>
                                        <td className="px-6 py-4 text-sm font-medium text-gray-900">{log.changedBy}</td>
                                        <td className="px-6 py-4 text-sm text-gray-600">
                                            {activeTab === 'ACCESS' ? (
                                                <span className="font-medium text-gray-700">
                                                    {log.affectedUserEmail === 'Viewed Logs' ? 'Admin Access' : log.affectedUserEmail}
                                                </span>
                                            ) : (
                                                log.affectedUserEmail !== 'N/A' ? log.affectedUserEmail : '-'
                                            )}
                                        </td>

                                        {activeTab === 'CHANGES' && (
                                            <td className="px-6 py-4 text-sm">
                                                <div className="flex flex-col gap-1">
                                                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-800 w-fit">
                                                        Before: {log.previousRole}
                                                    </span>
                                                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-800 w-fit">
                                                        After: {log.newRole}
                                                    </span>
                                                </div>
                                            </td>
                                        )}

                                        <td className="px-6 py-4 text-xs text-gray-500">
                                            <div className="flex items-center gap-1" title="Source / IP">{log.source} • {log.ipAddress}</div>
                                            <div className="truncate max-w-[150px]" title={log.userAgent}>{log.userAgent}</div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                {/* Pagination */}
                <div className="bg-gray-50 px-6 py-4 border-t border-gray-200 flex items-center justify-between">
                    <span className="text-sm text-gray-600">
                        Page <span className="font-medium">{page + 1}</span> of <span className="font-medium">{totalPages || 1}</span>
                    </span>
                    <div className="flex gap-2">
                        <button
                            onClick={() => fetchLogs(page - 1, true)} // Pagination skips logging
                            disabled={page === 0}
                            className="p-2 rounded-lg border border-gray-300 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                        >
                            <ChevronLeft className="w-5 h-5" />
                        </button>
                        <button
                            onClick={() => fetchLogs(page + 1, true)} // Pagination skips logging
                            disabled={page >= totalPages - 1}
                            className="p-2 rounded-lg border border-gray-300 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                        >
                            <ChevronRight className="w-5 h-5" />
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default AuditLogTable
