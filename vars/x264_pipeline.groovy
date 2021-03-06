

def executeBuild(String target, Map options)
{
    echo "executeBuild ${target}"
    
    dir("common_scripts")
    {
        cis_checkout_scm('master', "https://github.com/amfdev/common_scripts.git")
    }
    
    dir(options['projectName'])
    {
        cis_checkout_scm(options['projectBranch'], options['projectRepo'])
    }

    dir("${options.projectName}_scripts")
    {
        cis_checkout_scm('master', "https://github.com/amfdev/${options.projectName}_scripts.git")
        dir('build')
        {
            if("${target}" == "mingw_msvc_x64")
            {
                bat"""
                    set INCLUDE=C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\VC\\INCLUDE;C:\\Program Files (x86)\\Windows Kits\\8.1\\Include\\um\\;C:\\Program Files (x86)\\Windows Kits\\8.1\\Include\\shared\\;C:\\Program Files (x86)\\Windows Kits\\10\\include\\10.0.10240.0\\ucrt
                    set LIB=C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\VC\\LIB\\amd64;C:\\Program Files (x86)\\Windows Kits\\8.1\\lib\\winv6.3\\um\\x64;C:\\Program Files (x86)\\Windows Kits\\10\\Lib\\10.0.10150.0\\ucrt\\x64
                    ubuntu run sh -c './build.sh ${target}' >> ${CIS_LOG} 2>&1
                """
            }else
            if("${target}" == "mingw_msvc_x86")
            {
                bat"""
                    set INCLUDE=C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\VC\\INCLUDE;C:\\Program Files (x86)\\Windows Kits\\8.1\\Include\\um\\;C:\\Program Files (x86)\\Windows Kits\\8.1\\Include\\shared\\;C:\\Program Files (x86)\\Windows Kits\\10\\include\\10.0.10240.0\\ucrt
                    set LIB=C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\VC\\LIB;C:\\Program Files (x86)\\Windows Kits\\8.1\\lib\\winv6.3\\um\\x86;C:\\Program Files (x86)\\Windows Kits\\10\\Lib\\10.0.10150.0\\ucrt\\x86
                    ubuntu run sh -c './build.sh ${target}' >> ${CIS_LOG} 2>&1
                """
            }
            else
            {
                bat"""
                    ubuntu run sh -c './build.sh ${target}' >> ${CIS_LOG} 2>&1
                """
            }
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
            String projectName='x264',
            String projectRepo='https://github.com/amfdev/x264.git'
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
