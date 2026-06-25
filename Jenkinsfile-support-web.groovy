pipeline {
    agent any
    environment {
        APP_NAME = 'ele-ai-tender-support-web'
        APP_DIR = 'ele-ai-tender-support-frontend'

        HTML_REPO = 'E:\\java\\tender-document-tool\\frontend\\nginx-1.28.3\\html'
        BAK_REPO = 'E:\\java\\tender-document-tool\\bak'
    }
    tools {
        nodejs "NodeJS22.21.1"
    }
    options {
        buildDiscarder(logRotator(daysToKeepStr: '3', numToKeepStr: '10'))
        disableConcurrentBuilds()
        timeout(time: 15, unit: 'MINUTES')
    }
    stages {
        stage('Checkout') {
            steps {
                script {
                    // 确定目标服务器用户@IP
                    env.TARGET_SERVER = '10.11.20.50'
                    env.APP_ENV = 'test'

                    echo "[INFO] Building server: ${env.TARGET_SERVER}"
                    echo "[INFO] Building branch: ${env.BRANCH_NAME}"
                    echo "[INFO] Building env: ${env.APP_ENV}"

                    // 获取项目所在盘符
                    env.APP_DRIVE = bat(
                            script: "@echo off & for /f \"delims=\\\" %%i in (\"${env.HTML_REPO}\") do echo %%i",
                            returnStdout: true
                    ).trim()
                }
            }
        }
        stage('Init') {
            steps {
                script {
                    echo "--------------------------------📦 安装依赖: ${env.APP_DIR}--------------------------------"
                    bat """
                        cd ${env.APP_DIR}
                        nvm use 22.21.1
                        npm ci
                        npm install
                    """
                }
            }
        }
        stage('Build') {
            steps {
                script {
                    echo "--------------------------------🔨 构建: ${env.APP_DIR}--------------------------------"
                    bat """
                        cd ${env.APP_DIR}
                        npm run build
                    """
                }
            }
        }
        stage('Dryrun') {
            steps {
                script {
                    bat """
                        @echo off
                        setlocal enabledelayedexpansion

                        cd ${env.APP_DIR}
                        "C:\\Program Files\\7-Zip\\7z.exe" a -tzip ../${env.APP_NAME}.zip dist

                        endlocal
                    """
                    echo "--------------------------------✅ 打包完成: ${env.APP_NAME}.zip--------------------------------"
                }
            }
        }
        stage('Deploy') {
            steps {
                script {
                    echo "--------------------------------📤 开始部署: ${env.APP_NAME}--------------------------------"

                    sshPublisher(publishers: [
                            sshPublisherDesc(
                                    configName: "${env.TARGET_SERVER}",
                                    transfers: [
                                            sshTransfer(
                                                    sourceFiles: "${env.APP_NAME}.zip",
                                                    removePrefix: "",
                                                    remoteDirectory: "${env.HTML_REPO}",
                                                    execCommand: "E:\\20\\web.bat ${env.APP_NAME} ${env.APP_DRIVE} ${env.HTML_REPO} ${env.BAK_REPO}"
                                            )
                                    ],
                                    verbose: true
                            )
                    ])

                    echo "--------------------------------✅ 部署成功--------------------------------"
                }
            }
        }
    }
}
