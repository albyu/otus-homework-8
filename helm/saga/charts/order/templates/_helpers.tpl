{{- define "order.name" -}}             
{{- default .Values.order.name "order" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "order.fullname" -}}
{{- $name := default .Values.order.name "order" }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

