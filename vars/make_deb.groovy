def call(Map userInfo = [:], Map files, List deps, String copyright = "") {
	Map info = [
        Name:'app',
		Package:'app',
		Version:'1.0-1',
		Maintainer:"name <mail@gmail.com>",
		Architecture:"all",
		Section:"misc",
		Priority: "optional",
		Description:"short description" + System.getProperty("line.separator") +
		" long description line 1. line2"
		
    ]
    
    userInfo.each()
    {
        info[it.key]=it.value
    }
	
	String name = info['Name']
	String fullName = name + "_" + info['Version'] + "_" + info['Architecture']
	
	echo "###### Create deb: ${name} (${fullName}) ###############################"
	dir(name)
	{
		files.each()
		{
			dir(it.value)
			{
				unstash(it.key)
			}
		}
		dir("debian")
		{
			String control = ""
			info.each()
			{
				control += it.key + ": " + it.value + System.getProperty("line.separator")
			}
			if (deps.size() > 0)
			{
				control +="Depends: "
				int i =0;
				deps.each()
				{
					control +=it
					i++
					if (i < deps.size())
						control += ", "
				}
				control += System.getProperty("line.separator")
			}
			writeFile file: "control" , text: control
			if (copyright != "")
				writeFile file: "copyright" , text: copyright
			
		}
	}
	sh 'fakeroot dpkg-deb --build ' + name
	sh 'mv ' + name + '.deb ' + fullName + '.deb'
	//validate package
	sh 'lintian ' + fullName + '.deb'
}
