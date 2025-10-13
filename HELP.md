# 🚀 Projeto Spring Boot no Kubernetes com ArgoCD, GitHub Actions e Observabilidade (Prometheus + Grafana)

Este documento reúne **todo o passo a passo** e **troubleshootings** do projeto Kubernetes com java.  
Ele cobre desde a criação do cluster, deploy via ArgoCD, CI/CD com GitHub Actions, até a configuração completa de **observabilidade** com Prometheus e Grafana.

---

## 🧱 Estrutura dos Repositórios

- **Repositório 1:** `java-k8s` — código-fonte da aplicação Spring Boot.
- **Repositório 2:** `java-k8s-manifests` — manifests Kubernetes e integrações de observabilidade.

Ambos integrados com:
- **GitHub Actions** → Build & Push de imagem Docker no GitHub Container Registry.
- **ArgoCD** → Deploy contínuo da aplicação.
- **Kubernetes (kind/eks)** → Cluster local ou cloud.

---

## ⚙️ Aplicação Spring Boot

- Usa Java 21 e Spring Boot 3.x.
- Inclui endpoint `/actuator/prometheus` para métricas.
- Exemplo de `application.yaml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus, info
  endpoint:
    prometheus:
      enabled: true
  metrics:
    tags:
      application: java-k8s-app
````

Verificação local:

```bash
kubectl exec -it java-k8s-app-xxx -n java-k8s -- curl -s localhost:8080/actuator/prometheus | head
```

---

## 🐳 Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

---

## 🧩 Deployment Kubernetes (`deployment.yaml`)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: java-k8s-app
  namespace: java-k8s
  labels:
    app: java-k8s-app
spec:
  replicas: 2
  selector:
    matchLabels:
      app: java-k8s-app
  template:
    metadata:
      labels:
        app: java-k8s-app
    spec:
      containers:
        - name: my-app
          image: ghcr.io/rodrigonascimento001/java-k8s/my-app:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
      imagePullSecrets:
        - name: ghcr-secret
```

---

## 🌐 Service e Ingress

### `service.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: java-k8s-app
  namespace: java-k8s
spec:
  selector:
    app: java-k8s-app
  type: NodePort
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
      nodePort: 30081
```

### `ingress.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: java-k8s-app
  namespace: java-k8s
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
    - host: java-k8s.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: java-k8s-app
                port:
                  number: 80
```

---

## 🧠 ArgoCD Application

`argocd-application.yaml`

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: java-k8s-app
  namespace: argocd
spec:
  destination:
    name: ''
    namespace: java-k8s
    server: 'https://kubernetes.default.svc'
  source:
    repoURL: 'https://github.com/rodrigonascimento001/java-k8s-manifests'
    targetRevision: develop
    path: my-app
  project: default
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

---

## ⚙️ GitHub Actions CI/CD (`.github/workflows/ci-cd-argo.yaml`)

```yaml
name: CI/CD ArgoCD

on:
  push:
    branches:
      - develop

permissions:
  contents: write
  packages: write

jobs:
  build-and-push:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout source
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven

      - name: Build JAR
        run: mvn clean package -DskipTests

      - name: Login to GHCR
        uses: docker/login-action@v2
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push Docker image
        run: |
          IMAGE_TAG=${{ github.sha }}
          IMAGE_NAME=ghcr.io/${{ github.repository }}/java-k8s:${IMAGE_TAG}
          docker build -t $IMAGE_NAME .
          docker push $IMAGE_NAME
          echo "IMAGE_TAG=$IMAGE_TAG" >> $GITHUB_ENV

      - name: Checkout manifests repo
        uses: actions/checkout@v4
        with:
          repository: rodrigonascimento001/java-k8s-manifests
          token: ${{ secrets.GH_PAT }}
          path: java-k8s-manifests

      - name: Update image in deployment.yaml
        run: |
          cd java-k8s-manifests
          git config user.name "ci-bot"
          git config user.email "ci-bot@github.com"
          DEPLOYMENT_FILE=my-app/deployment.yaml
          sed -i "s|image:.*|image: ghcr.io/${{ github.repository }}/java-k8s:${IMAGE_TAG}|g" $DEPLOYMENT_FILE
          git add $DEPLOYMENT_FILE
          git commit -m "chore: update image to $IMAGE_TAG [ci skip]" || echo "No changes"
          git push
```

---

## ☁️ Conectando ao EKS

```bash
aws eks update-kubeconfig --region us-east-1 --name meu-cluster
kubectl get nodes
```

Caso veja erro `No resources found`, verifique:

* O cluster está ativo no console.
* Seu usuário IAM tem permissão de `eks:DescribeCluster` e `eks:UpdateKubeconfig`.

---

## 🔍 Observabilidade

### Instalação do Prometheus + Grafana

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install monitoring-stack prometheus-community/kube-prometheus-stack -n monitoring --create-namespace
```

Verifique:

```bash
kubectl get pods -n monitoring
```

---

### ServiceMonitor

`monitoring/serviceMonitor-java-k8s-app.yaml`

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: java-k8s-app-servicemonitor
  namespace: java-k8s
spec:
  selector:
    matchLabels:
      app: java-k8s-app
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
```

**⚠️ Erro comum:**

> `no matches for kind "ServiceMonitor" in version "monitoring.coreos.com/v1"`

✅ Solução: instale os CRDs do Prometheus Operator antes:

```bash
kubectl apply --server-side -f https://github.com/prometheus-operator/prometheus-operator/releases/latest/download/bundle.yaml
```

---

## 📊 Grafana

### Acesso

```bash
kubectl port-forward svc/monitoring-stack-grafana -n monitoring 3000:80
```

Acesse:
👉 [http://localhost:3000](http://localhost:3000)

* **Usuário:** `admin`
* **Senha:** execute:

  ```bash
  kubectl get secret monitoring-stack-grafana -n monitoring -o jsonpath="{.data.admin-password}" | base64 --decode
  ```

---

## 🧮 Criando Dashboard Spring Boot

1. Acesse Grafana → “+ Create” → “Dashboard”.
2. Adicione um **Painel**.
3. Query Prometheus:

   ```
   http_server_requests_seconds_count{application="java-k8s-app"}
   ```
4. Configure títulos, eixos e intervalos.
5. Salve como `Spring Boot - Metrics`.

---

## ✉️ Alertas por E-mail

Configure o Alertmanager:

```yaml
receivers:
  - name: email-alert
    email_configs:
      - to: seuemail@dominio.com
        from: alertmanager@monitoring.local
        smarthost: smtp.gmail.com:587
        auth_username: seuemail@dominio.com
        auth_identity: seuemail@dominio.com
        auth_password: "senha_app"
```

---

## 🧩 Troubleshooting

### 🔴 Ingress CrashLoopBackOff

* Verifique se o `ingressClassName: nginx` existe:

  ```bash
  kubectl get ingressclass
  ```
* Caso contrário, reinstale o Ingress Controller:

  ```bash
  helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx -n ingress-nginx --create-namespace
  ```

### 🔴 Grafana sem dados da aplicação

* Confirme:

  ```bash
  kubectl get servicemonitor -n java-k8s
  kubectl exec -it <pod> -n java-k8s -- curl -s localhost:8080/actuator/prometheus | head
  ```
* Verifique se o Prometheus tem target:

  ```
  http://localhost:9090/targets
  ```

---

## ✅ Resultado Final

Com essa configuração:

* **Spring Boot** publica métricas em `/actuator/prometheus`
* **Prometheus Operator** coleta via `ServiceMonitor`
* **Grafana** exibe dashboards personalizados
* **GitHub Actions** publica novas imagens automaticamente
* **ArgoCD** sincroniza automaticamente no cluster (local ou EKS)
* **Alertmanager** envia alertas por e-mail

---

## 📦 Próximos Passos

* Adicionar **Loki** (logs centralizados com Grafana).
* Integrar **Tempo + Traces** (observabilidade 3 pilares).
* Criar **Dashboards customizados** com templates do Grafana Labs.

---

**Autor:** Rodrigo Nascimento
**Data:** Outubro/2025
**Stack:** Spring Boot | Kubernetes | ArgoCD | Prometheus | Grafana | GitHub Actions | EKS

```