import itertools
import queue
inp = '_ + _ * _**2 + _**3 - _ == 399'

# coin puzzle
coins = [2, 3, 5, 7, 9]
for perm in itertools.permutations(coins):
    a, b, c, d, e = perm
    if a + b * c**2 + d**3 - e == 399:
        print(a, b, c, d, e)


# vault puzzle
dx = [-1, 0, 0, 1]
dy = [0, -1, 1, 0]
DIR = 'NWES'
OPER = '*-+'

n = 4
grid = [['*', '8', '-', '1'],
        ['4', '*', '11', '*'],
        ['+', '4', '-', '18'],
        ['22', '-', '9', '*']]


Q = queue.Queue()
Q.put(([(3, 0)], '', 22, ''))

while not Q.empty():
    moves, command, value, op = Q.get()
    last_move = moves[-1]
    if last_move == (0, 3):
        if value == 30:
            print(command)
            break
        continue
    for k in range(4):
        next_move = (last_move[0] + dx[k], last_move[1] + dy[k])
        if next_move != (3, 0) and 0 <= next_move[0] <= 3 and 0 <= next_move[1] <= 3:
            next_moves = moves[:]
            next_moves.append(next_move)

            next_op = grid[next_move[0]][next_move[1]]
            if OPER.find(next_op) != -1:
                Q.put((next_moves, command + DIR[k], value, next_op))
            else:
                next_value = eval(str(value) + op + next_op)
                Q.put((next_moves, command + DIR[k], next_value, ''))
