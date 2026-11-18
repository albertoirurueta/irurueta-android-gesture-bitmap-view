view.setImageFile(file)

// later, another component rewrites the file with an image of the same size
file.writeBytes(bytes)

// decode it again
view.invalidateImage()
