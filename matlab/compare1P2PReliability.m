function compare1P2PReliability(physical)
%Compare the flow reliablity of 1P and 2P
    file = '../log/revision/reliability/';
    if (physical)
        file = [file, 'physical/'];
    else
        file = [file, 'random/'];
    end
    file = [file, 'sc.txt'];
    x = importdata(file);
    data = x.data;
    figure;
    hold on;
    cdfplot(data(:, 1)*100);
    cdfplot(data(:, 2)*100);
    legend('1P', '2P');
    xlabel('Flow Reliablity (%)')
end