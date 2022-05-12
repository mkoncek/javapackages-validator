#!/bin/bash

rpmbuild -bb SPECS/*.spec -D "_topdir $(pwd)"
