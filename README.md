Simple ball maze controlled by the phone's gyroscope. Basic collision logic and simple velocity calculations with no real objective. Built for Android 10+.

<img width="344" height="774" alt="image" src="https://github.com/user-attachments/assets/5ee2a767-3f65-48e9-bae6-c6d70e91c8b5" />

App running on a Pixel 9a (API 36.1)

AI Disclosure:

While working on the ball movement logic, I wanted to make the ball have a bit more natural feel, so I asked Claude.ai how I could make it keep rolling for a time after the gyroscope values were updated. It suggested that I incorporate ball velocity affected by the gyroscope values rather than just using them to create position coordinates. I took some of what it said into account to help me update my code, but the code it gave me was very overcomplicated for such a simple use case. I incorporated a few lines that were relevant to the friction calculations for each frame, but the structure was more useful than the code itself, as it let me fill in my own logic once I knew how to make it fit.
