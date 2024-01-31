{{- define "store.name" -}}
{{- default .Values.store.name "store" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "store.fullname" -}}
{{- $name := default .Values.store.name "store" }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
