# About

This folder contains code for provisioning ACS with SearchServices or InsightEngine - for testing purposes.

# Content

```ruby
.
├── README.md                             # this readme file
└── search                                # code related to search product
    ├── Makefile                          # main Makefile (with common tasks)
    ├── backup                            # related to backup testing of Search Service
    │   ├── Makefile                      # start here if you want to test backup
    │   ├── README.md                     # how you can use it
    │   └── docker-compose.backup.yml     # overrides standard docker-compose.yml with backup data
    └── docker-compose.yml                # standard docker-compose.yml for Search Service
```
