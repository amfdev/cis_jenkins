
def executeBuild(String target, Map options)
{
    echo "executeBuild ${target}"
}

def executeTests(String target, String profile, Map options)
{
    echo "executeTests ${target} ${profile}"
}

def executeDeploy(Map configMap, Map options)
{
    echo "executeDeploy"
}

def call(String projectBranch = "", 
         String platforms = 'mingw_gcc_x64,mingw_gcc_x86,mingw_msvc_x64,mingw_msvc_x86', 
         String projectGroup='AMF',
         String projectName='AMF-FFmpeg',
         String projectRepo='https://github.com/amfdev/FFmpeg.git',
         Boolean updateRefs = false, 
         Boolean enableNotifications = false) {

    cis_multiplatform_pipeline(platforms, this.&executeBuild, this.&executeTests, this.&executeDeploy, 
                           [
                            projectBranch:projectBranch,
                            projectRepo:projectRepo,
                            projectGroup:projectGroup,
                            projectName:projectName,

                            'build.function':this.&executeBuild,
                            'test.function':this.&executeTest,
                            'deploy.function':this.&executeDeploy,

                            'build.tag':'BuilderAMF'
                            'test.tag':'Tester'
                            'deploy.tag':'DeployerAMF'
                           ])
}
