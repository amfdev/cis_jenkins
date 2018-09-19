def call(String name, Map files, List deps) {
	dir(name)
	{
		files.each()
		{
			dir(it.key)
			{
				unstash(it.value)
			}
		}
		dir("DEBIAN")
		{
			String control = "Package: " + name + System.getProperty("line.separator")
			control +="Version: 1.0-1" + System.getProperty("line.separator")
			control +="Maintainer: name >mail.gmail.com" + System.getProperty("line.separator")
			control +="Architecture: all" + System.getProperty("line.separator")
			control +="Section: misc" + System.getProperty("line.separator")
			control +="Description: short description" + System.getProperty("line.separator")
			control +=" long description line 1. line2" + System.getProperty("line.separator")
			control +="Depends: "
			deps.each()
			{
				control +=it
			}
			writeFile file: "control" , text: control
		}
	}
	bat '''
		wsl fakeroot dpkg-deb --build ${name}
	'''
	bat '''
		mv ${name}.deb ${name}_1.0-1_all.deb
	'''
}
