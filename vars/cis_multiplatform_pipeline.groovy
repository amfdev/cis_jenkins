
def executePlatform(String osName, String gpuNames, def executeBuild, def executeTests, def executeDeploy, Map options)
{
    def retNode =  
    {
        try {
            node("${osName} && ${options.BUILDER_TAG}")
            {
                stage("Build-${osName}")
                {
                    ws("WS/${options.PRJ_NAME}_Build")
                    {
                        try
                        {
                            if(options['cleanDirs'] == true)
                            {
                                echo 'cleaning directory'
                                deleteDir()
                            }

                            executeBuild(osName, options)
                        }
                        catch (e) {
                            currentBuild.result = "BUILD FAILED"
                            throw e
                        }
                        finally {
                            //archiveArtifacts "${STAGE_NAME}.log"
                        }
                    }
                }
            }

            if(gpuNames)
            {
                def testTasks = [:]
                gpuNames.split(',').each()
                {
                    String asicName = it
                    echo "Scheduling Test ${osName}:${asicName}"

                    testTasks["Test-${it}-${osName}"] = {
                        node("${osName} && ${options.TESTER_TAG} && gpu${asicName}")
                        {
                            stage("Test-${asicName}-${osName}")
                            {
                                ws("WS/${options.PRJ_NAME}_Test")
                                {
                                    try
                                    {
                                        if(options['cleanDirs'] == true)
                                        {
                                            echo 'cleaning directory'
                                            deleteDir()
                                        }

                                        Map newOptions = options.clone()
                                        newOptions['testResultsName'] = "testResult-${asicName}-${osName}"
                                        executeTests(osName, asicName, newOptions)
                                    }
                                    catch (e) {
                                        currentBuild.result = "BUILD FAILED"
                                        throw e
                                    }
                                    finally {
                                        //archiveArtifacts "${STAGE_NAME}.log"
                                    }
                                }
                            }
                        }
                    }
                }
                parallel testTasks
            }
            else
            {
                echo "No tests found for ${osName}"
            }
        }
        catch (e) {
            println(e.toString());
            println(e.getMessage());
            println(e.getStackTrace());        
            currentBuild.result = "FAILED"
            throw e
        }
    }
    return retNode
}

def call(String platforms, 
         def executeBuild, def executeTests, def executeDeploy, Map options) {
    
    try {
        
        properties([[$class: 'BuildDiscarderProperty', strategy: 
                     [$class: 'LogRotator', artifactDaysToKeepStr: '', 
                      artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);
        
        timestamps {
            String PRJ_PATH="${options.PRJ_ROOT}/${options.PRJ_NAME}"
            String REF_PATH="${PRJ_PATH}/ReferenceImages"
            String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
            options['PRJ_PATH']="${PRJ_PATH}"
            options['REF_PATH']="${REF_PATH}"
            options['JOB_PATH']="${JOB_PATH}"
            
            if(options.get('BUILDER_TAG', '') == '')
                options['BUILDER_TAG'] = 'Builder'

            if(options.get('DEPLOYER_TAG', '') == '')
                options['DEPLOYER_TAG'] = 'Deploy'

            if(options.get('TESTER_TAG', '') == '')
                options['TESTER_TAG'] = 'Tester'

            if(options.get('BUILD_CLEAN', '') == '')
                options['BUILD_CLEAN'] = 'false'

            if(options.get('TEST_CLEAN', '') == '')
                options['TEST_CLEAN'] = 'false'
            
            if(options.get('DEPLOY_CLEAN', '') == '')
                options['DEPLOY_CLEAN'] = 'true'
            
            def testResultList = [];

            try {
                
                def tasks = [:]

                echo "${platforms}"
                platforms.split(';').each()
                {
                    echo "${it}"
                    List tokens = it.tokenize(':')
                    String osName = tokens.get(0)
                    String gpuNames = tokens.get(1)
                    echo "${osName}"
                    echo "${gpuNames}"
                    if(gpuNames)
                    {
                        gpuNames.split(',').each()
                        {
                            String asicName = it
                            testResultList << "testResult-${asicName}-${osName}"
                        }
                    }
                    tasks[osName]=executePlatform(osName, gpuNames, executeBuild, executeTests, executeDeploy, options)
                }
                
                parallel tasks
                
            }
            finally
            {
                if(executeDeploy)
                {
                    node("Windows && ${options.DEPLOYER_TAG}")
                    {
                        stage("Deploy")
                        {
                            ws("WS/${options.PRJ_NAME}_Deploy")
                            {
                                try
                                {
                                    if(options['cleanDirs'] == true)
                                    {
                                        echo 'cleaning directory'
                                        deleteDir()
                                    }
                                    executeDeploy(options, testResultList)
                                }
                                catch (e) {
                                    println(e.toString());
                                    println(e.getMessage());
                                    println(e.getStackTrace());
                                    currentBuild.result = "DEPLOY FAILED"
                                    throw e
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        println(e.getStackTrace());
        currentBuild.result = "FAILED"
        throw e
    }
    finally {

        echo "enableNotifications = ${options.enableNotifications}"
        if("${options.enableNotifications}" == "true")
        {
            /*
            sendBuildStatusNotification(currentBuild.result, 
                                        options.get('slackChannel', ''), 
                                        options.get('slackBaseUrl', ''),
                                        options.get('slackTocken', ''))
                                        */
        }
    }
}
