import pandas
from math import sqrt
import sys


def rms(row):
    '''
    Calculates root mean square for a row of measurement
    '''
    sum = 0
    for elem in row:
        sum += elem * elem
    return(sqrt(sum))


def calc_bias(df):
    '''
    Calculates the bias for each column, estimated as the average of the accelerometer output
    '''
    bias = df.mean()
    return bias


def calc_noise(df, bias):
    '''
    Calculates the noise for the sensor, calculated by taking the root mean square of the signal
    '''
    # first remove the bias from the signal
    df_copy = df.copy()
    for i in range(3):
        df_copy[i] = df_copy[i] - bias[i]

    # then calculate rms (noise)
    df_copy['rms'] = df_copy.apply(lambda row: rms(row), axis=1)
    return df_copy['rms'].mean()


def main():
    # load data file from argv[1]
    df = pandas.read_csv(sys.argv[1], header=None)

    # calculate bias and noise
    bias = calc_bias(df)
    noise = calc_noise(df, bias)

    # print results
    print("Bias:", bias[0], bias[1], bias[2])
    print("Noise:", noise)


if __name__ == '__main__':
    main()
