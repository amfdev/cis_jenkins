

def executeBuild(String target, Map options)
{
    echo "executeBuild ${target}"
    
    dir("common_scripts")
    {
        cis_checkout_scm('master', "https://github.com/amfdev/common_scripts.git")
    }

    // build x264 (move to prebuild block)
    dir(options['projectName_x264'])
    {
        cis_checkout_scm(options['projectBranch_x264'], options['projectRepo_x264'])
    }

    dir("${options.projectName_x264}_scripts")
    {
        cis_checkout_scm('master', "https://github.com/amfdev/${options.projectName_x264}_scripts.git")
        dir('build')
        {
            bat"""
                ubuntu run sh -c './build.sh ${target}'
            """
        }
    }
    
    //build project
    dir(options['projectName'])
    {
        cis_checkout_scm(options['projectBranch'], options['projectRepo'])
    }

    dir("${options.projectName}_scripts")
    {
        cis_checkout_scm('master', "https://github.com/amfdev/${options.projectName}_scripts.git")
        dir('build')
        {
            bat"""
                ubuntu run sh -c './build.sh ${target}'
            """
        }
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
            String projectName='FFmpeg',
            String projectRepo='https://github.com/amfdev/FFmpeg.git',
            String projectName_x264='x264',
            String projectBranch_x264='master',
            String projectRepo_x264='https://github.com/amfdev/x264.git'
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
