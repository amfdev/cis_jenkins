

def executeBuild(String target, Map options)
{
    echo "executeBuild ${target}"
    
    dir('${options.projectName}_scripts')
    {
        cis_checkout_scm('master', 'https://github.com/amfdev/${options.projectName}_scripts.git')
    }
    
    dir(options['projectName'])
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

def call(
            String projectBranch = "", 
            String config = 'mingw_gcc_x64,mingw_gcc_x86,mingw_msvc_x64,mingw_msvc_x86', 
            String projectGroup='AMF',
            String projectName='x264',
            String projectRepo='http://git.videolan.org/git/x264.git'
        ) {

    Map options = [
        projectBranch:projectBranch,
        projectRepo:projectRepo,
        projectGroup:projectGroup,
        projectName:projectName,

        'build.function':this.&executeBuild,
        'deploy.function':this.&executeDeploy,

        'build.tag':'BuilderAMF',
        'build.platform.tag.mingw_gcc_x64':'mingw',
        'build.platform.tag.mingw_gcc_x86':'mingw',
        'build.platform.tag.mingw_msvc_x64':'mingw',
        'build.platform.tag.mingw_msvc_x86':'mingw',
       
        'deploy.tag':'DeployerAMF'
    ]
    
    cis_multiplatform_pipeline(config, options)
}
