
def buildHelper(String target)
{
    if("${target}" == "mingw_msvc_x64")
    {
        bat"""
            set PATH=C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\VC\\bin\\amd64;C:\\Program Files (x86)\\Windows Kits\\8.1\\bin\\x64;%PATH%
            set INCLUDE=C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\VC\\INCLUDE;C:\\Program Files (x86)\\Windows Kits\\8.1\\Include\\um\\;C:\\Program Files (x86)\\Windows Kits\\8.1\\Include\\shared\\;C:\\Program Files (x86)\\Windows Kits\\10\\include\\10.0.10240.0\\ucrt
            set LIB=C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\VC\\LIB\\amd64;C:\\Program Files (x86)\\Windows Kits\\8.1\\lib\\winv6.3\\um\\x64;C:\\Program Files (x86)\\Windows Kits\\10\\Lib\\10.0.10150.0\\ucrt\\x64
            ubuntu run sh -c './build.sh ${target}' >> ${CIS_LOG} 2>&1
        """
    }else
    if("${target}" == "mingw_msvc_x86")
    {
        bat"""
            set PATH=C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\VC\\bin;C:\\Program Files (x86)\\Windows Kits\\8.1\\bin\\x86;%PATH%
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


def executeBuild(String target, Map options)
{
    echo "executeBuild ${target}"
    
    dir("common_scripts")
    {
        cis_checkout_scm('master', "https://github.com/amfdev/common_scripts.git")
    }

    // build x264 (move to prebuild block)
    dir(options['projectName_AMF'])
    {
        cis_checkout_scm(options['projectBranch_AMF'], options['projectRepo_AMF'])
    }

    dir("${options.projectName_AMF}_scripts")
    {
        cis_checkout_scm('master', "https://github.com/amfdev/${options.projectName_AMF}_scripts.git")
        dir('build')
        {
            bat """
                ubuntu run sh -c './publish_headers.sh'
            """
        }
    }
    
    // build x264 (move to prebuild block)
    dir(options['projectName_x264'])
    {
        cis_checkout_scm(options['projectBranch_x264'], options['projectRepo_x264'])
    }
    dir("${options.projectName_x264}_redist")
    {
        deleteDir()
    }
    dir("${options.projectName_x264}_scripts")
    {
        cis_checkout_scm('master', "https://github.com/amfdev/${options.projectName_x264}_scripts.git")
        dir('build')
        {
            buildHelper(target)
        }
    }

    // build x265 (move to prebuild block)
    dir(options['projectName_x265'])
    {
        cis_checkout_scm(options['projectBranch_x265'], options['projectRepo_x265'])
    }
    dir("${options.projectName_x265}_redist")
    {
        deleteDir()
    }
    dir("${options.projectName_x265}_scripts")
    {
        cis_checkout_scm('master', "https://github.com/amfdev/${options.projectName_x265}_scripts.git")
        dir('build')
        {
            buildHelper(target)
        }
    }
    
    //build project
    dir(options['projectName'])
    {
        cis_checkout_scm(options['projectBranch'], options['projectRepo'])
    }
    dir("${options.projectName}_redist")
    {
        deleteDir()
    }
    dir("${options.projectName}_scripts")
    {
        cis_checkout_scm('master', "https://github.com/amfdev/${options.projectName}_scripts.git")
        dir('build')
        {
            buildHelper(target)
        }
    }

    dir("${options.projectName}_redist/${target}")
    {
        bat "echo ${target} > testout.txt"
        stash includes: '**/*', name: "app-${target}"
    }
}

def executeTests(String target, String profile, Map options)
{
    dir(target)
    {
        unstash "app-${target}"
        dir('bin')
        {
            bat "echo executeTests ${target}-${profile} >> ${CIS_LOG} 2>&1"
            bat "ffmpeg.exe -version >> ${CIS_LOG} 2>&1"
        }
    }
}

def executeDeploy(Map configMap, Map options)
{
    configMap.each()
    {
        dir(it.key)
        {
            unstash "app-${it.key}"
            dir('bin')
            {
                bat "echo deploy ${it.key} >> ${CIS_LOG} 2>&1"
                bat "ffmpeg.exe -version >> ${CIS_LOG} 2>&1"
            }
        }
    }
}

def call(Map userOptions = [:]
        ) {

    Map options = [
        config:'mingw_gcc_x64,mingw_gcc_x86:gpuAMD_RXVEGA',
        
        projectGroup:'AMF',

        projectName:'FFmpeg',
        projectBranch:'master',
        projectRepo:'https://github.com/amfdev/FFmpeg.git',

        projectName_x264:'x264',
        projectBranch_x264:'master',
        projectRepo_x264:'https://github.com/amfdev/x264.git',

        projectName_x265:'x265',
        projectBranch_x265:'master',
        projectRepo_x265:'https://github.com/amfdev/x265.git',

        projectName_AMF:'AMF',
        projectBranch_AMF:'master',
        projectRepo_AMF:'https://github.com/GPUOpen-LibrariesAndSDKs/AMF.git',
        
        'build.function':this.&executeBuild,
        'test.function':this.&executeTests,
        'deploy.function':this.&executeDeploy,

        'build.tag':'BuilderAMF',
        'build.platform.tag.mingw_gcc_x64':'mingw',
        'build.platform.tag.mingw_gcc_x86':'mingw',
        'build.platform.tag.mingw_msvc_x64':'mingw',
        'build.platform.tag.mingw_msvc_x86':'mingw',

        'test.tag':'TesterAMF',
        'test.cleandir':true,
        'test.platform.tag.mingw_gcc_x64':'Windows',
        'test.platform.tag.mingw_gcc_x86':'Windows',
        'test.platform.tag.mingw_msvc_x64':'Windows',
        'test.platform.tag.mingw_msvc_x86':'Windows',
        
        'deploy.tag':'DeployerAMF',
        'deploy.cleandir':true
    ]
    
    userOptions.each()
    {
        options[it.key]=it.value
    }
    
    cis_multiplatform_pipeline(options)
}
