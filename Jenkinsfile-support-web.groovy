pipeline {
    agent any
    environment {
        APP_NAME = 'ele-ai-tender-support-web'
        APP_DIR = 'ele-ai-tender-support-frontend'

        HTML_REPO = 'E:\\20\\Nginx\\html'
        BAK_REPO = 'E:\\20\\ele-ai-tender_bak'
    }
    tools {
        nodejs "NodeJS18.17.0"
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
                    def envType = env.BRANCH_NAME.split('_')[0]
                    if (envType == 'dev') {
                        env.TARGET_SERVER = '10.11.20.42'
                        env.APP_ENV = 'dev'
                    } else if (envType == 'test') {
                        env.TARGET_SERVER = '10.11.20.50'
                        env.APP_ENV = 'test'
                    } else if (envType == 'master') {
                        env.TARGET_SERVER = '10.11.20.50'
                        env.APP_ENV = 'test'
                    } else if (envType == 'releases') {
                        env.TARGET_SERVER = '10.11.20.50'
                        env.APP_ENV = 'test'
                    } else {
                        error "[ERROR] 分支名错误"
                    }

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
                        npm install
                        npm audit fix
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
