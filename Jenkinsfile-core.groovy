pipeline {
    agent any
    environment {
        APP_NAME = 'ele-ai-tender-core'
        APP_PORT = '8082'

        APP_REPO = 'E:\\java\\tender-document-tool\\ele-ai-tender-core'
        BAK_REPO = 'E:\\java\\tender-document-tool\\bak'

        // JAR 选项
        JAR_OPTS = '-Xmx1G -Dfile.encoding=utf-8'
        // 最大尝试次数
        MAX_ATTEMPTS = 10
        // 等待时间
        INTERVAL = 30
    }
    tools {
        maven "Maven"
        jdk "JDK21"
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
                            script: "@echo off & for /f \"delims=\\\" %%i in (\"${env.APP_REPO}\") do echo %%i",
                            returnStdout: true
                    ).trim()
                }
            }
        }
        stage('Build') {
            steps {
                script {
                    echo "--------------------------------📦 开始构建, 项目: ${env.APP_NAME}--------------------------------"

                    // 构建common + 当前服务模块（-am 自动构建上游依赖）
                    bat """
                        cd ele-ai-tender-system
                        mvn clean package -pl ${env.APP_NAME} -am -DskipTests
                    """

                    env.JAR_NAME = bat(
                            script: "@echo off && for /f \"delims=\" %%i in ('dir /b ele-ai-tender-system\\${env.APP_NAME}\\target\\*-exec.jar ^|^| echo File Not Found') do echo %%i",
                            returnStdout: true
                    ).trim()

                    if (env.JAR_NAME != 'File Not Found') {
                        echo "[INFO] JAR 文件构建完成: ${env.JAR_NAME}"
                        echo "--------------------------------✅ 构建完成, JAR包名为: ${env.JAR_NAME}--------------------------------"
                    } else {
                        echo "[ERROR] JAR 文件构建失败"
                        error "--------------------------------❌ 构建失败, JAR 文件未找到--------------------------------"
                    }
                }
            }
        }
        stage('Deploy') {
            steps {
                script {
                    echo "--------------------------------📤 部署 JAR 文件: ${env.JAR_NAME}--------------------------------"

                    sshPublisher(publishers: [
                            sshPublisherDesc(
                                    configName: "${env.TARGET_SERVER}",
                                    transfers: [
                                            sshTransfer(
                                                    sourceFiles: "ele-ai-tender-system/${env.APP_NAME}/target/${env.JAR_NAME}",
                                                    removePrefix: "ele-ai-tender-system/${env.APP_NAME}/target",
                                                    remoteDirectory: "${env.APP_REPO}\\jenkins-transfer-temp",
                                                    execCommand: "E:\\java\\process-jdk21.bat ${env.APP_NAME} ${env.APP_PORT} ${env.APP_DRIVE} ${env.APP_REPO} ${env.BAK_REPO} ${env.JAR_NAME} \"${env.JAR_OPTS}\""
                                            )
                                    ],
                                    verbose: true
                            )
                    ])

                    echo "[INFO] finish: ${env.JAR_NAME} to ${env.APP_NAME}-${env.APP_PORT}"
                    echo "--------------------------------✅ JAR 文件部署成功--------------------------------"
                }
            }
        }
        stage('Check') {
            steps {
                script {
                    echo "--------------------------------📤 检查程序启动情况--------------------------------"

                    def maxAttempts = env.MAX_ATTEMPTS.toInteger()
                    def interval = env.INTERVAL.toInteger()
                    def success = false
                    for (int i = 0; i < maxAttempts; i++) {
                        try {
                            echo "第 ${i + 1} 次健康检查尝试..."
                            def response = httpRequest(url: "http://${env.TARGET_SERVER}:${env.APP_PORT}/actuator/health",
                                    httpMode: 'GET',
                                    validResponseCodes: '200:299',
                                    timeout: 10
                            )
                            echo "✅ 健康检查成功! 响应状态: ${response.status}"
                            success = true
                            break
                        } catch (Exception exception) {
                            echo "❌ 第 ${i + 1} 次健康检查失败: ${exception.getMessage()}"
                            if (i < maxAttempts - 1) {
                                echo "等待 ${interval} 秒后重试..."
                                sleep(interval)
                            }
                        }
                    }
                    if (!success) {
                        error("健康检查失败: 在 ${maxAttempts * interval} 秒内未收到成功响应")
                    }
                }
            }
        }
    }
}
