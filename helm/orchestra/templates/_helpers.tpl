{{- define "orchestra.name" -}}
{{- default .Values.orchestra.name "orchestra" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "orchestra.fullname" -}}
{{- $name := default .Values.orchestra.name "orchestra" }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
