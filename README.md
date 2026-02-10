# ecochain-ci

Jenkins Shared Library for ecochain CI/CD.

## Usage

```groovy
@Library('ecochain-ci') _
javaMavenCI {
  appName = 'order-service'
  enableScan = true
}
