ALF_VERSION=$1

if [ ! -f $ALF_VERSION.env ]; then
    echo "Version \"$ALF_VERSION\" not found!"
    echo "Available versions are:" 
    echo "$(ls *.env | awk '{ print substr( $0, 0, length($0)-4 ) }')"
    exit 1
fi

echo "Copying .env for version $ALF_VERSION"
FOLDERS=$(find . -name 'docker-compose.yml' -exec dirname {} \;)

for d in $FOLDERS
do
	cp "$ALF_VERSION.env" $d/.env
done
