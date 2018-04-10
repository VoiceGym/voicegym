#!/bin/sh
# Download precompiled fftw3.zip
curl -L https://www.dropbox.com/s/bruz28e3cu1z3ch/fftw3.zip?dl=1 --output fftw3.zip
# Clean
rm -rf fftw3
# Unzip 
unzip fftw3.zip
# Remove Zip
rm -rf fftw3.zip

