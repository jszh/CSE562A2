import pandas
from math import sqrt
import matplotlib.pyplot as plt
import numpy as np


def main():
    # load data file from argv[1]
    df = pandas.read_csv('1651288557612-tilt.txt', header=None)

    print(len(df[0]))
    x = np.arange(0, len(df[0][::107]) / 4, 0.25)
    plt.plot(x, df[0][::107], label='Accelerometer')
    plt.plot(x, df[1][::107], label='Gyroscope')
    plt.plot(x, df[2][::107], label='Complementary')
    plt.ylim([0, 0.04])
    plt.xlabel('Time (s)')
    plt.ylabel('Tilt (rad)')
    plt.legend()
    plt.show()


if __name__ == '__main__':
    main()
