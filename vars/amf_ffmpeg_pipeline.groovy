
def executeBuild(String target, Map options)
{
    echo "executeBuild ${target}"
    
    cis_checkout_scm(options['projectBranch'], 'https://github.com/amfdev/ffmpeg_ws.git')
    
    dir('FFmpeg')
    {
        cis_checkout_scm(options['projectBranch'], options['projectRepo'])
    }
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
         String config = 'mingw_gcc_x64,mingw_gcc_x86,mingw_msvc_x64,mingw_msvc_x86', 
         String projectGroup='AMF',
         String projectName='AMF-FFmpeg',
         String projectRepo='https://github.com/amfdev/FFmpeg.git',
         Boolean updateRefs = false, 
         Boolean enableNotifications = false) {

    Map options = [
        projectBranch:projectBranch,
        projectRepo:projectRepo,
        projectGroup:projectGroup,
        projectName:projectName,

        'build.function':this.&executeBuild,
        'test.function':this.&executeTest,
        'deploy.function':this.&executeDeploy,

        'build.tag':'BuilderAMF',
        'build.platform.tag.mingw_gcc_x64':'mingw',
        'build.platform.tag.mingw_gcc_x86':'mingw',
        'build.platform.tag.mingw_msvc_x64':'mingw',
        'build.platform.tag.mingw_msvc_x86':'mingw',
        
        'test.tag':'Tester',
        'test.platform.tag.mingw_gcc_x64':'Windows',
        'test.platform.tag.mingw_gcc_x86':'Windows',
        'test.platform.tag.mingw_msvc_x64':'Windows',
        'test.platform.tag.mingw_msvc_x86':'Windows',
        
        'deploy.tag':'DeployerAMF'
    ]
    
    cis_multiplatform_pipeline(config, options)
}
