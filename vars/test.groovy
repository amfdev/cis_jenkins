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
    echo "-------------------------------------executeBuild ${target}-------------------------------------"
	cis_checkout_scm('master', "https://github.com/amfdev/HandBrake_dev")
	dir("Sources")
    {
        cis_checkout_scm('master', "https://github.com/amfdev/HandBrake.git")
    }
    
    echo "-----------------------------------------end----------------------------------------------------"
}

def executeTests(String target, String profile, Map options)
{
	echo "-------------------------------------executeTests ${target}-------------------------------------"
    
    echo "-----------------------------------------end----------------------------------------------------"
}

def executeDeploy(Map configMap, Map options)
{
	echo "-------------------------------------executeDeploy ${target}-------------------------------------"
    
    echo "-----------------------------------------end----------------------------------------------------"
}

def call(Map userOptions = [:]
        ) {

    Map options = [
        config:'test:test',
        
        projectGroup:'AMF',

        projectName:'HB',
        projectBranch:'master',
        projectRepo:'https://github.com/amfdev/HandBrake_dev.git',
        
        'build.function':this.&executeBuild,
        'test.function':this.&executeTests,
        'deploy.function':this.&executeDeploy,

        'build.tag':'test',
        'build.platform.tag.mingw_gcc_x64':'mingw',
        'build.platform.tag.mingw_gcc_x86':'mingw',
        'build.platform.tag.mingw_msvc_x64':'mingw',
        'build.platform.tag.mingw_msvc_x86':'mingw',

        'test.tag':'test',
        'test.cleandir':true,
        'test.platform.tag.mingw_gcc_x64':'Windows',
        'test.platform.tag.mingw_gcc_x86':'Windows',
        'test.platform.tag.mingw_msvc_x64':'Windows',
        'test.platform.tag.mingw_msvc_x86':'Windows',
        
        'deploy.tag':'test',
        'deploy.cleandir':true
    ]
    
    userOptions.each()
    {
        options[it.key]=it.value
    }
    
    cis_multiplatform_pipeline(options)
}