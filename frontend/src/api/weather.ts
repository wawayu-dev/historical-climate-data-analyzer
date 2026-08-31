import axios from 'axios'
import type { ApiResponse, Metadata, TemperatureAnomaly, UploadResult, WeatherFilters } from '../types/weather'

const client = axios.create({
  baseURL: '/api/weather',
  timeout: 20_000,
})

const filterParams = (filters: WeatherFilters) => ({
  regions: filters.regions.length ? filters.regions.join(',') : undefined,
  startYear: filters.startYear ?? undefined,
  endYear: filters.endYear ?? undefined,
  month: filters.month ?? undefined,
})

export async function uploadWeatherCsv(file: File): Promise<UploadResult> {
  const form = new FormData()
  form.append('file', file)
  const response = await client.post<ApiResponse<UploadResult>>('/upload', form)
  return response.data.data
}

export async function getMetadata(): Promise<Metadata> {
  const response = await client.get<ApiResponse<Metadata>>('/metadata')
  return response.data.data
}

export async function getResults(filters: WeatherFilters): Promise<TemperatureAnomaly[]> {
  const response = await client.get<ApiResponse<TemperatureAnomaly[]>>('/results', {
    params: filterParams(filters),
  })
  return response.data.data
}

export async function exportResults(filters: WeatherFilters): Promise<void> {
  const response = await client.get<Blob>('/export', {
    params: filterParams(filters),
    responseType: 'blob',
  })
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = 'temperature-anomaly.csv'
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const payload = error.response?.data as Partial<ApiResponse<unknown>> | undefined
    if (payload?.message) return payload.message
    if (error.code === 'ECONNABORTED') return '请求超时，请稍后重试'
    if (!error.response) return '无法连接后端服务，请确认服务已启动'
  }
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}
