ALF_VERSION=$1
echo "Copying .env for version $ALF_VERSION"
FOLDERS=$(find . -name 'docker-compose.yml' -exec dirname {} \;)

for d in $FOLDERS
do
	cp "$ALF_VERSION.env" $d/.env
done
