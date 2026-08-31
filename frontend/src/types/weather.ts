export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface UploadResult {
  rawRecordCount: number
  resultCount: number
  regions: string[]
  minYear: number | null
  maxYear: number | null
  abnormalCount: number
  warnings: string[]
}

export interface Metadata extends UploadResult {
  months: number[]
}

export interface TemperatureAnomaly {
  year: number
  month: number
  region: string
  monthlyAverage: number
  baselineAverage: number
  anomaly: number
}

export interface WeatherFilters {
  regions: string[]
  startYear: number | null
  endYear: number | null
  month: number | null
}

export const emptyMetadata = (): Metadata => ({
  rawRecordCount: 0,
  resultCount: 0,
  regions: [],
  minYear: null,
  maxYear: null,
  months: [],
  abnormalCount: 0,
  warnings: [],
})
