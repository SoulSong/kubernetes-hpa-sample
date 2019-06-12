# Introduction
This is a sample of showing how to autoScale pods with the CPU usage. 

# Require
- First of all, HPA depends on the metrics-server. If you did not deploy it already, please 
refer to the [monitor sample repo](https://github.com/SoulSong/kubernetes-monitor-sample).
- Second, if we want to trigger the hpa feature with the CPU usage, we have to define the limit resource of CPU. 

Here are three ways to define the cpu limitation:
- [limitRange-container.yaml](./kubernetes/limitRange-container.yaml)
> This is the definition for each container.
- [limitRange-pod.yaml](./kubernetes/limitRange-pod.yaml)
> This is the definition for each pod. Each pod may contain more than one container.
- [deployment.yaml](./kubernetes/deployment.yaml)
> Directly defined in the pod template as follows:
```yaml
    spec:
      containers:
        - name: hpa-service
          image: local-dtr.com/kubernetes-hpa-sample:Develop
          ports:
            - containerPort: 8080
              name: http
          resources:
            limits:
              cpu: 200m
            requests:
              cpu: 101m
```
 
# Deploy
```bash
mvn clean install && kubectl create -f ./kubernetes/
```

# Test

### Check metric-server
Get the cpu and memory usage of all pods:
```bash
$ kubectl top pods -n hpa-sample
NAME                      CPU(cores)   MEMORY(bytes)
hpa-dm-69cb57c8d4-gxgf8   200m         204Mi
hpa-dm-69cb57c8d4-mhfmz   200m         260Mi
hpa-dm-69cb57c8d4-rtlxq   200m         212Mi
```
When the java applications are starting, they use `100%` cpu resources. We know that CPU is the compressible resource.
We had setting the limitation of the CPU usage less than or equal to `200m` before then. So that means our Settings are working.When 
When the java applications already started, watch the cpu usage:
```bash
$ kubectl top pods -n hpa-sample
NAME                      CPU(cores)   MEMORY(bytes)
hpa-dm-69cb57c8d4-gxgf8   2m           304Mi
hpa-dm-69cb57c8d4-mhfmz   2m           330Mi
hpa-dm-69cb57c8d4-rtlxq   2m           299Mi
```
They use a bit of the CPU resources and more memory resource.

### Check endpoint
Here are three pods, they exposed port with the `service` resource which using the `LoadBalancer` type. 
Try many time:
```bash
$ curl 127.0.0.1:8880/hello
Hello, i am hpa-dm-69cb57c8d4-mhfmz.
$ curl 127.0.0.1:8880/hello
Hello, i am hpa-dm-69cb57c8d4-rtlxq.
$ curl 127.0.0.1:8880/hello
Hello, i am hpa-dm-69cb57c8d4-mhfmz.
$ curl 127.0.0.1:8880/hello
Hello, i am hpa-dm-69cb57c8d4-gxgf8.
```

### Setting autoscale
Execute `kubectl autoscale **` command as follows:
```bash
$ kubectl autoscale deployment hpa-dm --min=2 --max=5 --cpu-percent=15 -n hpa-sample
```
At the same time, open more screens to monitor the status of the deployment and events.
```bash
$ kubectl get events -n hpa-sample -w
LAST SEEN   TYPE     REASON                  OBJECT                         MESSAGE
18m         Normal   Created                 pod/hpa-dm-69cb57c8d4-rtlxq    Created container
18m         Normal   Started                 pod/hpa-dm-69cb57c8d4-rtlxq    Started container
18m         Normal   SuccessfulCreate        replicaset/hpa-dm-69cb57c8d4   Created pod: hpa-dm-69cb57c8d4-gxgf8
18m         Normal   SuccessfulCreate        replicaset/hpa-dm-69cb57c8d4   Created pod: hpa-dm-69cb57c8d4-mhfmz
18m         Normal   SuccessfulCreate        replicaset/hpa-dm-69cb57c8d4   Created pod: hpa-dm-69cb57c8d4-rtlxq
18m         Normal   ScalingReplicaSet       deployment/hpa-dm              Scaled up replica set hpa-dm-69cb57c8d4 to 3
0s          Normal   SuccessfulRescale       horizontalpodautoscaler/hpa-dm   New size: 2; reason: All metrics below target
0s          Normal   ScalingReplicaSet       deployment/hpa-dm                Scaled down replica set hpa-dm-69cb57c8d4 to 2
1s          Normal   SuccessfulDelete        replicaset/hpa-dm-69cb57c8d4     Deleted pod: hpa-dm-69cb57c8d4-gxgf8
0s          Normal   Killing                 pod/hpa-dm-69cb57c8d4-gxgf8      Killing container with id docker://hpa-service:Need to kill Pod
```
The last four lines show that the `horizontalpodautoscaler` trigger the auto scale capacity.
```bash
$ kubectl get deployment hpa-dm -n hpa-sample -w
NAME     READY   UP-TO-DATE   AVAILABLE   AGE
hpa-dm   3/3     3            3           24m
hpa-dm   3/2     3            3           28m
hpa-dm   3/2     3            3           28m
hpa-dm   2/2     2            2           28m
```
At last there are 2 available pods after finishing the auto scale.
View the change of the hpa, output shows the metrics value which it is monitoring.
```bash
$ kubectl get hpa  -w -n  hpa-sample
NAME     REFERENCE           TARGETS         MINPODS   MAXPODS   REPLICAS   AGE
hpa-dm   Deployment/hpa-dm   <unknown>/15%   2         5         0          0s
hpa-dm   Deployment/hpa-dm   1%/15%          2         5         3          30s
hpa-dm   Deployment/hpa-dm   1%/15%          2         5         2          60s
```
Through `kubectl get hpa -o yaml -n hpa-sample`, it will show how the hpa defined.
```yaml
apiVersion: autoscaling/v1
kind: HorizontalPodAutoscaler
metadata:
  name: hpa-dm
  namespace: hpa-sample
spec:
  maxReplicas: 5
  minReplicas: 2
  scaleTargetRef:
    apiVersion: extensions/v1beta1
    kind: Deployment
    name: hpa-dm
  targetCPUUtilizationPercentage: 15
```
Above just show the main definition of hpa and delete useless information.

# Others
In addition to using the HPA functionality on the command line, it can also be defined using yaml, see [more](./kubernetes/hpa/hpa.yaml).
```bash
$ kubectl apply -f ./kubernetes/hpa/hpa.yaml
```

# Cleanup
```bash
$ kubectl delete namespace hpa-sample
```