function feasibles=loadFeasibilityData(data, ch)
    channels = data(:, 6);
    ids = find(channels == ch);
    %receive all data with channel = ch.
    data = data(ids, :);
    %x is all data of schedulability
    x = data(:, 7:5:size(data, 2));
    %consider check failure as normal unschedulable.
    x(x == -1) = 0;
    feasibles = sum(x);
    feasibles = feasibles / size(x, 1);
    feasibles = feasibles(:, [1:5, 9, 7:8, 10, 11, 12]);
end